/*
 * Copyright (C) 2009-2020 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.http.scaladsl.model.ws

import java.lang.Iterable

import scala.collection.immutable
import akka.NotUsed
import akka.annotation.DoNotInherit
import akka.stream._
import akka.http.impl.util.JavaMapping
import akka.http.javadsl.model.ws
import akka.http.javadsl.{ model => jm }
import akka.http.scaladsl.model.HttpResponse
import akka.stream.scaladsl.{ Flow, Sink, Source }

/**
 * A custom header that will be added to an WebSocket upgrade HttpRequest that
 * enables a request handler to upgrade this connection to a WebSocket connection and
 * registers a WebSocket handler.
 */
@DoNotInherit
trait UpgradeToWebSocket extends jm.ws.UpgradeToWebSocket {
  /**
   * A sequence of protocols the client accepts.
   *
   * See http://tools.ietf.org/html/rfc6455#section-1.9
   */
  def requestedProtocols: immutable.Seq[String]

  /**
   * The high-level interface to create a WebSocket server based on "messages".
   *
   * Returns a response to return in a request handler that will signal the
   * low-level HTTP implementation to upgrade the connection to WebSocket and
   * use the supplied handler to handle incoming WebSocket messages.
   *
   * Optionally, a subprotocol out of the ones requested by the client can be chosen.
   */
  def handleMessages(
    handlerFlow: Graph[FlowShape[Message, Message], Any],
    subprotocol: Option[String]                          = None): HttpResponse

  /**
   * The high-level interface to create a WebSocket server based on "messages".
   *
   * Returns a response to return in a request handler that will signal the
   * low-level HTTP implementation to upgrade the connection to WebSocket and
   * use the supplied inSink to consume messages received from the client and
   * the supplied outSource to produce message to sent to the client.
   *
   * Optionally, a subprotocol out of the ones requested by the client can be chosen.
   */
  def handleMessagesWithSinkSource(
    inSink:      Graph[SinkShape[Message], Any],
    outSource:   Graph[SourceShape[Message], Any],
    subprotocol: Option[String]                   = None): HttpResponse =
    handleMessages(scaladsl.Flow.fromSinkAndSource(inSink, outSource), subprotocol)

  import scala.collection.JavaConverters._

  /**
   * Java API
   */
  def getRequestedProtocols(): Iterable[String] = requestedProtocols.asJava

  /**
   * Java API
   */
  def handleMessagesWith(handlerFlow: Graph[FlowShape[jm.ws.Message, jm.ws.Message], _ <: Any]): HttpResponse =
    handleMessages(JavaMapping.toScala(handlerFlow))

  /**
   * Java API
   */
  def handleMessagesWith(handlerFlow: Graph[FlowShape[jm.ws.Message, jm.ws.Message], _ <: Any], subprotocol: String): HttpResponse =
    handleMessages(JavaMapping.toScala(handlerFlow), subprotocol = Some(subprotocol))

  /**
   * Java API
   */
  def handleMessagesWith(inSink: Graph[SinkShape[jm.ws.Message], _ <: Any], outSource: Graph[SourceShape[jm.ws.Message], _ <: Any]): HttpResponse =
    handleMessages(createScalaFlow(inSink, outSource))

  /**
   * Java API
   */
  def handleMessagesWith(
    inSink:      Graph[SinkShape[jm.ws.Message], _ <: Any],
    outSource:   Graph[SourceShape[jm.ws.Message], _ <: Any],
    subprotocol: String): HttpResponse =
    handleMessages(createScalaFlow(inSink, outSource), subprotocol = Some(subprotocol))

  override def handleMessagesWithSink(sink: Graph[SinkShape[jm.ws.Message], _ <: Any]): HttpResponse =
    handleMessages(createScalaCoupledFlow(sink, Source.maybe))

  override def handleMessagesWithSource(source: Graph[SourceShape[jm.ws.Message], _ <: Any])(implicit mat: Materializer): HttpResponse =
    handleMessages(createScalaCoupledFlow(akka.http.javadsl.model.ws.WebSocket.ignoreSink(mat), scaladsl.Source.fromGraph(source)))

  private[this] def createScalaFlow(inSink: Graph[SinkShape[jm.ws.Message], _ <: Any], outSource: Graph[SourceShape[jm.ws.Message], _ <: Any]): Graph[FlowShape[Message, Message], NotUsed] =
    JavaMapping.toScala(scaladsl.Flow.fromSinkAndSourceMat(inSink, outSource)(scaladsl.Keep.none): Graph[FlowShape[jm.ws.Message, jm.ws.Message], NotUsed])

  private[this] def createScalaCoupledFlow(inSink: Graph[SinkShape[jm.ws.Message], _ <: Any], outSource: Graph[SourceShape[jm.ws.Message], _ <: Any]): Graph[FlowShape[Message, Message], NotUsed] = {
    ??? // I really can't get this to compile... :-| JavaMapping.toScala(scaladsl.CoupledTerminationFlow.fromSinkAndSource(Sink.fromGraph(inSink), Source.fromGraph(outSource)).mapMaterializedValue((v: (Any, Any)) ⇒ NotUsed): Graph[FlowShape[Message, Message], NotUsed])
  }
}
