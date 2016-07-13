package org.apache.mesos.edsl
import org.apache.mesos.edsl.{scheduler => S}
import org.apache.mesos.edsl.{data => D}
import org.apache.mesos.{Protos => P}
import org.apache.{mesos => M}
import org.apache.mesos.edsl.{monad => E}
import org.apache.mesos.edsl.monad._
import scala.concurrent.{Channel}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global


/**
  * Created by Jesus E. Larios Murillo on 6/24/16.
  */
object SchedulerMTest {

  def main(args: Array[String]): Unit = {
    val name = "SchedulerMTest " + System.currentTimeMillis()
    val user = "" // take the default
    val checkpointing = false
    val timeout = 60.0
    val id = P.FrameworkID.newBuilder.setValue(name).build()

    val executorCommand = P.CommandInfo.newBuilder
      .setValue("/opt/mesosphere/bin/java -cp /opt/mesosphere/bin/SleepFramework-assembly-1.0.jar SchedulerMExecutor")
      .build()
    val executorId = P.ExecutorID.newBuilder.setValue("SchedulerMExecutor-" + System.currentTimeMillis())
    val executorName = "SchedulerM Executor"
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

    def newTask(cpu:Double, mem:Double):P.TaskInfo = {
        val id = P.TaskID.newBuilder.setValue("SchedulerMTask" + System.currentTimeMillis())
        val name = id.getValue
        val cpuR = P.Resource.newBuilder.setName("cpus").setType(P.Value.Type.SCALAR).setScalar(P.Value.Scalar.newBuilder.setValue(cpu))
        val memR = P.Resource.newBuilder.setName("mem").setType(P.Value.Type.SCALAR).setScalar(P.Value.Scalar.newBuilder.setValue(mem))
        P.TaskInfo.newBuilder
          .setExecutor(executor)
          .setName(name)
          .setTaskId(id)
          .addResources(cpuR)
          .addResources(memR)
          .build()
      }


    def command(cpu:Double, mem:Double, c:String):E.SchedulerM[String] = for {
      t <- E.launch(newTask(cpu,mem))
      _ <- E.isRunning(t)
      _ <- E.sendTaskMsg(t,c.getBytes)
      r <- E.recvTaskMsg(t) 
    } yield(new String(r))

    Future {
      driver.run()
    }
    val script:E.SchedulerM[String] = for {
      s <- command(10, 2048, "uname -a") orElse command(1, 128, "uname -a")
      _ <- E.shutdown
    } yield(s)

    val s = script.run(D.SchedulerState(driver, channel, List()))
    println(s)
    sys.exit(0)
  }
}
