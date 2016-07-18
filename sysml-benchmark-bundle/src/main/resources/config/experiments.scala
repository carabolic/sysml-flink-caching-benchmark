package config

import com.typesafe.config.ConfigFactory
import org.peelframework.core.beans.data.{CopiedDataSet, DataSet, ExperimentOutput, GeneratedDataSet}
import org.peelframework.core.beans.experiment.ExperimentSuite
import org.peelframework.hadoop.beans.experiment.YarnExperiment
import org.peelframework.spark.beans.experiment.SparkExperiment
import org.peelframework.spark.beans.system.Spark
import org.peelframework.flink.beans.system.Flink
import org.peelframework.flink.beans.experiment.FlinkExperiment
import org.peelframework.hadoop.beans.system.{Yarn, HDFS2}
import org.springframework.context.annotation._
import org.springframework.context.{ApplicationContext, ApplicationContextAware}

/** Experiments definitions for the 'sysml-benchmark' bundle. */
@Configuration
@ComponentScan( // Scan for annotated Peel components in the 'eu.stratosphere.benchmarks.systemml' package
  value = Array("eu.stratosphere.benchmarks.systemml"),
  useDefaultFilters = false,
  includeFilters = Array[ComponentScan.Filter](
    new ComponentScan.Filter(value = Array(classOf[org.springframework.stereotype.Service])),
    new ComponentScan.Filter(value = Array(classOf[org.springframework.stereotype.Component]))
  )
)
@ImportResource(value = Array(
  "classpath:peel-core.xml",
  "classpath:peel-extensions.xml"
))
@Import(value = Array(
  classOf[org.peelframework.extensions], // custom system beans
  classOf[config.fixtures.systems],      // custom system beans
  classOf[config.fixtures.datasets]
))
class experiments extends ApplicationContextAware {

  val runs = 1

  /* The enclosing application context. */
  var ctx: ApplicationContext = null

  def setApplicationContext(ctx: ApplicationContext): Unit = {
    this.ctx = ctx
  }

  // ---------------------------------------------------
  // Experiments
  // ---------------------------------------------------

  @Bean(name = Array("linreg.generate.data"))
  def `linreg.generate.data`: ExperimentSuite = {
    val `linreg.generate.data.spark` = new SparkExperiment(
      name    = "linreg.generate.data.spark",
      command =
        s"""
           |--class org.apache.sysml.api.DMLScript \\
           |$${app.path.apps}/SystemML.jar \\
           |-f $${app.path.apps}/scripts/utils/splitXY.dml \\
           |-nvargs X=$${system.hadoop-2.path.output}/linRegData.csv \\
           |y=51 OX=$${system.hadoop-2.path.output}/linRegData.train.data.csv \\
           |OY=$${system.hadoop-2.path.output}/linRegData.train.labels.csv ofmt=csv
         """.stripMargin.trim,
      config  = ConfigFactory.parseString(""),
      runs    = 1,
      runner  = ctx.getBean("spark-1.6.0", classOf[Spark]),
      inputs  = Set(ctx.getBean("linreg.dataset.features", classOf[DataSet])),
      outputs = Set()
    )

    new ExperimentSuite(Seq(
    `linreg.generate.data.spark`
    ))
  }

  @Bean(name = Array("linreg.train.ds"))
  def `linreg.train.ds`: ExperimentSuite = {
    val `linreg.train.spark` = new SparkExperiment(
      name    = "linreg.train.spark",
      command =
        s"""
           |--class org.apache.sysml.api.DMLScript \\
           |$${app.path.apps}/SystemML.jar \\
           |-f $${app.path.apps}/scripts/algorithms/LinearRegDS.dml -nvargs \\
           |X=$${system.hadoop-2.path.output}/linRegData.train.data.csv \\
           |Y=$${system.hadoop-2.path.output}/linRegData.train.labels.csv \\
           |B=$${system.hadoop-2.path.output}/betas.csv fmt=csv
         """.stripMargin.trim,
      config = ConfigFactory.parseString(""),
      runs   = runs,
      runner = ctx.getBean("spark-1.6.0", classOf[Spark]),
      inputs = Set(),
      outputs = Set()
    )

    val `linreg.train.flink` = new FlinkExperiment(
      name    = "linreg.train.flink",
      command =
        s"""
           |-c org.apache.sysml.api.DMLScript \\
           |$${app.path.apps}/SystemML.jar \\
           |-f $${app.path.apps}/scripts/algorithms/LinearRegDS.dml -nvargs \\
           |X=$${system.hadoop-2.path.output}/linRegData.train.data.csv \\
           |Y=$${system.hadoop-2.path.output}/linRegData.train.labels.csv \\
           |B=$${system.hadoop-2.path.output}/betas.csv fmt=csv
         """.stripMargin.trim,
      config = ConfigFactory.parseString(""),
      runs   = runs,
      runner = ctx.getBean("flink-1.0.3", classOf[Flink]),
      inputs = Set(),
      outputs = Set()
    )

    val `linreg.train.yarn` = new YarnExperiment(
      name    = "linreg.train.yarn",
      command =
        s"""
           |jar $${app.path.apps}/SystemML.jar \\
           |org.apache.sysml.api.DMLScript \\
           |-f $${app.path.apps}/scripts/algorithms/LinearRegDS.dml -nvargs \\
           |X=$${system.hadoop-2.path.output}/linRegData.train.data.csv \\
           |Y=$${system.hadoop-2.path.output}/linRegData.train.labels.csv \\
           |B=$${system.hadoop-2.path.output}/betas.csv fmt=csv
         """.stripMargin.trim,
      config = ConfigFactory.parseString(""),
      runs   = runs,
      runner = ctx.getBean("yarn-2.7.1", classOf[Yarn]),
      inputs = Set(),
      outputs = Set(),
      systems = Set()
    )

    new ExperimentSuite(Seq(
      `linreg.train.spark`,
      `linreg.train.flink`,
      `linreg.train.yarn`
    ))
  }
}