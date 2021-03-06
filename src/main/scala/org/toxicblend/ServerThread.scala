package org.toxicblend

import java.nio.charset.Charset
import java.net.{ InetAddress, ServerSocket, Socket, SocketException }
import java.util.Random
import java.io.{ DataOutputStream, DataInputStream, EOFException, IOException }

import org.toxicblend.protobuf.ToxicBlendProtos.Message
import org.toxicblend.protobuf.ToxicBlendProtos.{ Option => MessageOption }
import org.toxicblend.protobuf.ToxicBlendProtos.Model
import org.toxicblend.typeconverters.OptionConverter
import org.toxicblend.operations.volumetricmesh.VolumetricMeshOperation
import org.toxicblend.operations.projectionoutline.ProjectionOutlineOperation
import org.toxicblend.operations.boostmedianaxis.MedianAxisOperation
import org.toxicblend.operations.dragoncurve.DragonCurveOperation
import org.toxicblend.operations.simplify.SimplifyOperation
import org.toxicblend.operations.simplegcodegenerator.SimpleGcodeGeneratorOperation
import org.toxicblend.operations.simplegcodeparse.SimpleGcodeParseOperation
import org.toxicblend.operations.saveobj.SaveObjOperation
import org.toxicblend.operations.zadjust.ZAdjustOperation
import org.toxicblend.operations.generatemaze.GenerateMazeOperation
import org.toxicblend.operations.intersectedges.IntersectEdgesOperation
import org.toxicblend.operations.parametricmodels.ParametricCircleOperation
import org.toxicblend.operations.offset2dshape.Offset2dShapeOperation
import org.toxicblend.operations.subdivideedges.SubdivideEdgesOperation
import org.toxicblend.operations.meshgenerator.MeshGeneratorOperation
import org.toxicblend.util.Time
import akka.actor.ActorSystem
import com.google.protobuf.{ CodedInputStream, CodedOutputStream }
import toxi.geom.AABB
import toxi.geom.Vec3D

object ServerThread {
  private val CHARSET = Charset.forName("UTF-8")
}

case class ServerThread(private val actorSystem: ActorSystem, socket: Socket) extends Thread("ServerThread") {
  var lastActiveTime = System.nanoTime

  override def run(): Unit = {
    //val rand = new Random(System.currentTimeMillis());
    var commandsReceived = 0 // the number of messages sent from the client on this specific connection

    try {
      val out = new DataOutputStream(socket.getOutputStream)
      val in = new DataInputStream(socket.getInputStream)
      var inputData: Array[Byte] = new Array[Byte](100)

      while (true) {
        do {
          val (inMessage, inLength) = Time.time("Received command: ", {
            // Receive data from client
            val inLength = in.readInt
            if (inLength > inputData.length) {
              inputData = new Array[Byte](inLength)
            }
            in.readFully(inputData, 0, inLength)
            val codedInputStream = CodedInputStream.newInstance(inputData, 0, inLength);
            val inMessage = Message.parseFrom(codedInputStream)
            (inMessage, inLength)
          })
          println("Server received command: \"" + inMessage.getCommand() + "\" of " + inLength + " bytes.")

          val outMessage = {
            try {
              val processor: CommandProcessorTrait = inMessage.getCommand() match {
                case "OBJECT_OT_toxicblend_volume" => new VolumetricMeshOperation
                case "OBJECT_OT_toxicblend_add_dragon_curve" => new DragonCurveOperation
                case "OBJECT_OT_toxicblend_projection_outline" => new ProjectionOutlineOperation(actorSystem)
                case "OBJECT_OT_toxicblend_medianaxis" => new MedianAxisOperation
                case "OBJECT_OT_toxicblend_simplify" => new SimplifyOperation
                case "OBJECT_OT_toxicblend_simplegcodegenerator" => new SimpleGcodeGeneratorOperation
                case "OBJECT_OT_toxicblend_simplegcodeviewer" => new SimpleGcodeParseOperation
                case "OBJECT_OT_toxicblend_saveobj" => new SaveObjOperation
                case "OBJECT_OT_toxicblend_zadjust" => new ZAdjustOperation
                case "OBJECT_OT_toxicblend_generatemaze" => new GenerateMazeOperation
                case "OBJECT_OT_toxicblend_intersectedges" => new IntersectEdgesOperation
                case "OBJECT_OT_toxicblend_addparametriccircleoperation" => new ParametricCircleOperation
                case "OBJECT_OT_toxicblend_offset2d" => new Offset2dShapeOperation
                case "OBJECT_OT_toxicblend_subdivideedges" => new SubdivideEdgesOperation
                case "OBJECT_OT_toxicblend_meshgenerator" => new MeshGeneratorOperation
                case s: String => {
                  val errMsg = "Unknown command: " + s
                  System.err.println(errMsg)
                  throw new ToxicblendException(errMsg)
                }
              }
              val options = OptionConverter(inMessage)
              processor.processInput(inMessage, options)
            } catch {
              case e: Throwable => {
                val message = Message.newBuilder()
                val optionBuilder = MessageOption.newBuilder()
                optionBuilder.setKey("ERROR")
                optionBuilder.setValue(e.toString)
                message.addOptions(optionBuilder)
                e.printStackTrace
                message
              }
            }
          }
          Option(outMessage.getCommand()).foreach(_ => outMessage.setCommand("None"))
          val output = outMessage.build.toByteArray()

          out.writeInt(output.length)
          out.write(output)
          out.flush()
          println("Response sent: " + output.length + " bytes")
          lastActiveTime = System.nanoTime
          commandsReceived += 1
        } while (true)

        Thread.sleep(50)
      }
      out.close
      in.close
      socket.close
    } catch {
      case e: SocketException =>
        () // avoid stack trace when stopping a client with Ctrl-C
      case e: EOFException =>
        if (commandsReceived == 0) {
          e.printStackTrace
        }
        println("Response received by Blender: " + Time.formatAsSeconds((System.nanoTime - lastActiveTime) / 1e6f))
        println("Closing connection to " + socket.getPort + "->" + socket.getLocalPort);
      case e: IOException =>
        e.printStackTrace
    }
  }
}