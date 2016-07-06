package com.svds.eeg.emotiv.kafka.streams

import com.svds.kafka.connect.opentsdb.Point
import java.util.Properties

import io.confluent.examples.streams.IntegrationTestUtils
import io.confluent.examples.streams.kafka.EmbeddedSingleNodeKafkaCluster
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.confluent.kafka.serializers.{AbstractKafkaAvroSerDeConfig, KafkaAvroDeserializer, KafkaAvroDeserializerConfig, KafkaAvroSerializer}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.{Serde, Serdes, StringDeserializer, StringSerializer}
import org.apache.kafka.streams.StreamsConfig
import org.scalatest._

import scala.collection.JavaConversions._

/**
  *
  * Test class based on Confluent's example tests, but using Spec style instead of xUnit style.
  *
  * @see https://github.com/confluentinc/examples/tree/master/kafka-streams
  *
  */
class StreamTransformerSpec extends FlatSpec with BeforeAndAfter {

  private val cluster = new EmbeddedSingleNodeKafkaCluster
  cluster.start()

  private val sensorsTopic = "sensors"
  private val eegTopic = "eeg"

  private val stringSerde = Serdes.String()

  private val streamsTestingConfiguration = {
    val p = new Properties()
    p.put(StreamsConfig.APPLICATION_ID_CONFIG, "com.svds.eeg.emotiv.kafka.streams.StreamTransformerSpec")
    p.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, cluster.bootstrapServers())
    p.put(StreamsConfig.ZOOKEEPER_CONNECT_CONFIG, cluster.zookeeperConnect())
    p.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, cluster.schemaRegistryUrl())
    val serdeClass = stringSerde.getClass
    p.put(StreamsConfig.KEY_SERDE_CLASS_CONFIG, serdeClass)
    p.put(StreamsConfig.VALUE_SERDE_CLASS_CONFIG, serdeClass)
    p.put(StreamsConfig.STATE_DIR_CONFIG, "/tmp/kafka-streams-unit-tests")
    p
  }

  private val producerConfig = new Properties()
  producerConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, cluster.bootstrapServers())
  producerConfig.put(ProducerConfig.ACKS_CONFIG, "all")
  producerConfig.put(ProducerConfig.RETRIES_CONFIG, 0.toString)
  producerConfig.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer])
  producerConfig.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer])

  private val consumerConfig = new Properties()
  consumerConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, cluster.bootstrapServers())
  consumerConfig.put(ConsumerConfig.GROUP_ID_CONFIG, "con.svds.eeg.emotiv.kafka.streams.StreamTransformerTestConsumer")
  consumerConfig.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
  consumerConfig.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer])
  consumerConfig.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, classOf[KafkaAvroDeserializer])
  consumerConfig.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, cluster.schemaRegistryUrl())
  consumerConfig.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true.toString)

  private val schemaRegistryClient = new CachedSchemaRegistryClient(streamsTestingConfiguration.getProperty(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG), 4)
  private val avroSerDe: Serde[AnyRef] = Serdes.serdeFrom(new KafkaAvroSerializer(schemaRegistryClient), new KafkaAvroDeserializer(schemaRegistryClient))

  before {
    cluster.createTopic(sensorsTopic)
    cluster.createTopic(eegTopic)

    IntegrationTestUtils.purgeLocalStreamsState(streamsTestingConfiguration)

    StreamTransformer.buildAndStartStreamingTopology(streamsTestingConfiguration, sensorsTopic, eegTopic, stringSerde, avroSerDe)
  }

  "StreamTransformer" should "downsample & convert each CSV row to multiple OpenTSDB Avro objects (ignoring the header)" in {
    val header = "COUNTER, AF3, F7, F3, FC5, T7, P7, O1, O2, P8, T8, FC6, F4, F8, AF4, GYROX, GYROY, TIMESTAMP, FUNC_ID, FUNC_VALUE, MARKER, SYNC_SIGNAL"
    val inputMessages = List(
      header,
      "0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21",
      "1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22"
    )
    IntegrationTestUtils.produceValuesSynchronously(sensorsTopic, inputMessages, producerConfig)
    val expectedNumberOfAvroObjectsPerCSVrow = 16
    val outputMessages = IntegrationTestUtils.waitUntilMinValuesRecordsReceived[Point](consumerConfig, eegTopic, expectedNumberOfAvroObjectsPerCSVrow)
    assert(outputMessages.size() == expectedNumberOfAvroObjectsPerCSVrow)
    val sampleMeasurement = outputMessages.head
    assert(sampleMeasurement.timestamp > 0)
    assert(!sampleMeasurement.metric.equals(""))
    assert(sampleMeasurement.value > 0.0)
  }

  after {
    cluster.stop()
  }
}
