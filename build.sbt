name := "eeg-kafka-streams"

version := "0.0.1"

scalaVersion := "2.11.8"

lazy val versions = Map(
  "kafka" -> "0.10.0.0",
  "confluent" -> "3.0.0",
  "scalaTest" -> "3.0.0-RC3"
)

libraryDependencies ++= Seq(
  "com.github.scopt" %% "scopt" % "3.5.0",
  "io.confluent" % "kafka-avro-serializer" % versions("confluent"),
  "io.confluent" % "kafka-schema-registry" % versions("confluent") % "test",
  "io.confluent" % "kafka-schema-registry" % versions("confluent") % "test" classifier "tests",
  "io.confluent" % "kafka-schema-registry-client" % versions("confluent"),
  "io.confluent" % "streams-examples" % versions("confluent") % "test" classifier "tests",
  "junit" % "junit" % "4.12" % "test",
  "org.apache.curator" % "curator-test" % "3.1.0" % "test",
  "org.apache.kafka" %% "kafka" % versions("kafka") % "test",
  "org.apache.kafka" %% "kafka" % versions("kafka") % "test" classifier "test",
  "org.apache.kafka" % "kafka-streams" % versions("kafka"),
  "org.scalactic" %% "scalactic" % versions("scalaTest"),
  "org.scalatest" %% "scalatest" % versions("scalaTest") % "test"
)

resolvers ++= Seq(
  "Artima" at "http://repo.artima.com/releases",
  "Confluent" at "http://packages.confluent.io/maven/",
  "Local" at s"file://${Path.userHome.absolutePath}/.m2/repository"
)

Seq(sbtavrohugger.SbtAvrohugger.specificAvroSettings : _*)

mainClass in assembly := Some("com.svds.eeg.emotiv.kafka.streams.StreamTransformer")
