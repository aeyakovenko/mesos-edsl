package org.apache.mesos.edsl
import org.apache.mesos.edsl.{scheduler => S}
import org.apache.mesos.edsl.{data => D}
import org.apache.mesos.{Protos => P}
import org.apache.{mesos => M}
import org.apache.mesos.edsl.{monad => E}
import org.apache.mesos.edsl.monad.{SchedulerMRun}
import scala.concurrent.{Channel}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import org.apache.mesos.edsl.executor.CommandExecutor


object SchedulerMTest {

  def main(args: Array[String]): Unit = {
    val name = "SchedulerMTest " + System.currentTimeMillis()
    val user = "" // take the default
    val checkpointing = false
    val timeout = 60.0
    val id = P.FrameworkID.newBuilder.setValue(name).build()

    val executorCommand = P.CommandInfo.newBuilder
      .setValue("/opt/mesosphere/bin/java -cp /vagrant/mesos-edsl-assembly-1.0.jar org.apache.mesos.edsl.executor.CommandExecutor")
      .build()
    val executorId = P.ExecutorID.newBuilder.setValue("CommandExecutor-" + System.currentTimeMillis())
    val executorName = "CommandExecutor Executor"
    val source = "java"


    val executor = P.ExecutorInfo.newBuilder
      .setCommand(executorCommand)
      .setExecutorId(executorId)
      .setName(executorName)
      .setSource(source)
      .build()
    val channel = new Channel[D.SchedulerEvents]()

    val scheduler = new S.Scheduler(channel)
    val framework = P.FrameworkInfo.newBuilder
      .setName(name)
      .setFailoverTimeout(timeout)
      .setCheckpoint(checkpointing)
      .setUser(user)
      .setId(id)
      .build()
    val mesosMaster = "192.168.65.90:5050"

    val driver = new M.MesosSchedulerDriver(scheduler, framework, mesosMaster)

    def newTask(cpu:Double, mem:Double, cmd:String):P.TaskInfo = {
        val id = P.TaskID.newBuilder.setValue("SchedulerMTask" + System.currentTimeMillis())
        val name = id.getValue
        val cpuR = P.Resource.newBuilder.setName("cpus").setType(P.Value.Type.SCALAR).setScalar(P.Value.Scalar.newBuilder.setValue(cpu))
        val memR = P.Resource.newBuilder.setName("mem").setType(P.Value.Type.SCALAR).setScalar(P.Value.Scalar.newBuilder.setValue(mem))
        P.TaskInfo.newBuilder
          .setExecutor(executor)
          .setName(name)
          .setTaskId(id)
          .setSlaveId(P.SlaveID.newBuilder.setValue("hello world").build())
          .setData(com.google.protobuf.ByteString.copyFrom(cmd.getBytes))
          .addResources(cpuR)
          .addResources(memR)
          .build()
      }

    Future {
      driver.run()
    }

    def command(s:P.TaskInfo):E.SchedulerM[String] = for {
      t <- E.launch(s)
      _ <- E.isRunning(t)
      r <- E.recvTaskMsg(t) 
      _ <- E.isFinished(t)
    } yield(new String(r))

    val programs:E.SchedulerM[String]  = {
      command(newTask(10, 2048, "uname -a")) orElse command(newTask(1, 128, "uname -a"))
    }

    val script:E.SchedulerM[String] = for {
      _ <- E.logln("starting...")
      _ <- E.registered
      _ <- E.logln("registered!")
      _ <- E.updateOffers
      _ <- E.logln("gotOffers!")
      s <- programs
      _ <- E.logln("ran program!")
      _ <- E.logln(s"program, output: $s")
      _ <- E.shutdown
    } yield(s)

    val s = script.run(D.SchedulerState(driver, channel, List(), None))
    println(s)
    sys.exit(0)
  }
}
