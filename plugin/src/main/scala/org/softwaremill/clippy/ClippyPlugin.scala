package org.softwaremill.clippy

import scala.reflect.internal.util.Position
import scala.tools.nsc.Global
import scala.tools.nsc.plugins.{PluginComponent, Plugin}
import scala.tools.nsc.reporters.Reporter

class ClippyPlugin(val global: Global) extends Plugin {

  override val name: String = "clippy"

  override val components: List[PluginComponent] = Nil

  override val description: String = "gives good advice"

  override def init(options: List[String], error: (String) => Unit) = {
    val r = global.reporter

    global.reporter = new Reporter {
      override protected def info0(pos: Position, msg: String, severity: Severity, force: Boolean) = ???

      override def echo(msg: String) = r.echo(msg)
      override def comment(pos: Position, msg: String) = r.comment(pos, msg)
      override def hasErrors = r.hasErrors || cancelled
      override def reset() = {
        cancelled = false
        r.reset()
      }

      //

      override def echo(pos: Position, msg: String) = r.echo(pos, msg)
      override def warning(pos: Position, msg: String) = r.warning(pos, msg)
      override def errorCount = r.errorCount
      override def warningCount = r.warningCount
      override def hasWarnings = r.hasWarnings
      override def flush() = r.flush()
      override def count(severity: Severity): Int = r.count(conv(severity))
      override def resetCount(severity: Severity): Unit = r.resetCount(conv(severity))

      //

      private def conv(s: Severity): r.Severity = s match {
        case INFO => r.INFO
        case WARNING => r.WARNING
        case ERROR => r.ERROR
      }

      //

      override def error(pos: Position, msg: String) = r.error(pos, handleError(pos, msg))
    }

    def handleError(pos: Position, msg: String): String = {
      if (msg.contains("akka.http.scaladsl.server.StandardRoute") &&
        msg.contains("akka.stream.scaladsl.Flow[akka.http.scaladsl.model.HttpRequest,akka.http.scaladsl.model.HttpResponse,Any]")) {
        msg + "\n" +
          """
            |Clippy advice:
            | did you forget to define an implicit akka.stream.ActorMaterializer?
            | It allows routes to be converted into a flow.
          """.stripMargin
      }
      else msg
    }

    true
  }
}
