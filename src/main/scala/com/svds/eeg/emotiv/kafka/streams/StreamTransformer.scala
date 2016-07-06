package com.svds.eeg.emotiv.kafka.streams

import java.lang.Iterable
import java.util.Properties

import com.svds.kafka.connect.opentsdb.Point
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.confluent.kafka.serializers.{AbstractKafkaAvroSerDeConfig, KafkaAvroDeserializer, KafkaAvroSerializer}
import org.apache.kafka.common.serialization.{Serde, Serdes}
import org.apache.kafka.streams.{KafkaStreams, StreamsConfig}
import org.apache.kafka.streams.kstream.{KStream, KStreamBuilder, Predicate, ValueMapper}

import scala.collection.JavaConversions._

/** command line options */
case class Config(bootstrapServers: Seq[String] = Seq("localhost:9092"), zookeeper: String = "localhost:2181", schemaRegistryUrlString: String = "http://localhost:8081", source: String = "sensors", sink: String = "eeg")

/** Converts each Emotiv CSV row to multiple OpenTSDB Avro objects */
object StreamTransformer extends App {

  val commandLineInputParser = new scopt.OptionParser[Config]("StreamTransformer") {
    head("StreamTransformer")

    opt[Seq[String]]("bootstrap-servers").required().action((serversInput, config) =>
      config.copy(bootstrapServers = serversInput)).text("comma-separate list (e.g., localhost:9092)")

    opt[String]("zookeeper").required().action((zookeeperInput, config) =>
      config.copy(zookeeper = zookeeperInput)).text("zookeeper host & port (e.g., localhost:2181)")

    opt[String]("schema-registry").required().action((schemaRegistryInput, config) =>
      config.copy(schemaRegistryUrlString = schemaRegistryInput)).text("URL of schema registry server (e.g., http://localhost:8081)")

    opt[String]("source").required().action((sourceTopicNameInput, config) =>
      config.copy(source = sourceTopicNameInput)).text("source topic name")

    opt[String]("sink").required().action((sinkTopicNameInput, config) =>
      config.copy(sink = sinkTopicNameInput)).text("destination topic name")
  }

  val parsedOptions = commandLineInputParser.parse(args, Config())

  if (parsedOptions.isEmpty) {
    System.exit(1)
  }

  val cliOptions = parsedOptions.orNull

  /* Kafka Streams application configuration */
  val config = new Properties
  config.put(StreamsConfig.APPLICATION_ID_CONFIG, "com.svds.eeg.emotiv.kafka.streams.StreamTransformer")
  config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, cliOptions.bootstrapServers.mkString(","))
  config.put(StreamsConfig.ZOOKEEPER_CONNECT_CONFIG, cliOptions.zookeeper)
  config.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, cliOptions.schemaRegistryUrlString);
  val stringSerde = Serdes.String()
  val serdeClass = stringSerde.getClass
  config.put(StreamsConfig.KEY_SERDE_CLASS_CONFIG, serdeClass)
  config.put(StreamsConfig.VALUE_SERDE_CLASS_CONFIG, serdeClass)

  /* In order to use Confluent's Avro serdes, the schema registry must be specified. */
  val schemaRegistryClient = new CachedSchemaRegistryClient(config.getProperty(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG), 4)
  val avroSerDe: Serde[AnyRef] = Serdes.serdeFrom(new KafkaAvroSerializer(schemaRegistryClient), new KafkaAvroDeserializer(schemaRegistryClient))

  def buildAndStartStreamingTopology(config: Properties, sourceTopicName: String, sinkTopicName: String, sinkKeySerde: Serde[String], sinkValueSerde: Serde[AnyRef]) = {
    val builder: KStreamBuilder = new KStreamBuilder

    val textLines: KStream[String, String] = builder.stream(sourceTopicName)

    val downSampled = textLines.filter(new Predicate[String, String]() {
      override def test(key: String, value: String): Boolean = {
        try {
          val counter = value.split(',').head.toDouble
          counter % 16 == 0.0
        } catch {
          case numberFormatException: NumberFormatException => false
        }
      }
    })

    val transformed = downSampled.flatMapValues(new ValueMapper[String, Iterable[AnyRef]]() {
      override def apply(value: String) = {
        val Array(
        counter,
        af3,
        f7,
        f3,
        fc5,
        t7,
        p7,
        o1,
        o2,
        p8,
        t8,
        fc6,
        f4,
        f8,
        af4,
        gyrox,
        gyroy,
        timestamp,
        funcID,
        funcValue,
        marker,
        syncSignal
        ) = value.split(',')

        List(("af3", af3),
          ("f7", f7),
          ("f3", f3),
          ("fc5", fc5),
          ("t7", t7),
          ("p7", p7),
          ("o1", o1),
          ("o2", o2),
          ("p8", p8),
          ("t8", t8),
          ("fc6", fc6),
          ("f4", f4),
          ("f8", f8),
          ("af4", af4),
          ("gyrox", gyrox),
          ("gyroy", gyroy)
        ).map { case (k, v) =>
          Point(s"eeg.${k}",
            System.currentTimeMillis(),
            v.toDouble,
            Map("sensor" -> k))
        }
      }
    })
    transformed.to(sinkKeySerde, sinkValueSerde, sinkTopicName)

    val streams = new KafkaStreams(builder, config)
    streams.start()
  }

  buildAndStartStreamingTopology(config, cliOptions.source, cliOptions.sink, stringSerde, avroSerDe)
}