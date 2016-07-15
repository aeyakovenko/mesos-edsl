package org.apache.mesos.edsl.executor

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import org.apache.{mesos => M}
import org.apache.mesos.{Protos => P}
import sys.process._ //for !! todo: can you import operators?
import java.nio.charset.StandardCharsets.UTF_8

/**
 * simple executor implementaion that launches a command as a system call and sends the output as a framework message
 */
object CommandExecutor extends M.Executor {
  override def shutdown(driver: M.ExecutorDriver): Unit = {
    println("Shutdown: starting")
  }

  override def disconnected(driver: M.ExecutorDriver): Unit = println("Disconnected: ")

  override def killTask(driver: M.ExecutorDriver, taskId: P.TaskID): Unit = println("Kill Task: ")

  override def reregistered(driver: M.ExecutorDriver, slaveInfo: P.SlaveInfo): Unit = println("Reregistered: ")

  override def error(driver: M.ExecutorDriver, message: String): Unit = println("Error: in error mode")

  override def frameworkMessage(driver: M.ExecutorDriver, data: Array[Byte]): Unit = println("frameworkMessage: ")

  override def registered(driver: M.ExecutorDriver, executorInfo: P.ExecutorInfo, frameworkInfo: P.FrameworkInfo, slaveInfo: P.SlaveInfo): Unit = println("Registered: ")

  override def launchTask(driver: M.ExecutorDriver, task: P.TaskInfo): Unit = {
    val cmd = task.getData.toStringUtf8
    println(s"launchedTask cmd: $cmd")
    val thread = new Thread {
      override def run(): Unit = {

        driver.sendStatusUpdate(P.TaskStatus.newBuilder
          .setTaskId(task.getTaskId)
          .setState(P.TaskState.TASK_RUNNING).build())
        val res = cmd !!

        driver.sendFrameworkMessage(res.getBytes)

        driver.sendStatusUpdate(P.TaskStatus.newBuilder
          .setTaskId(task.getTaskId)
          .setState(P.TaskState.TASK_FINISHED)
          .build())
      }
    }

    thread.start()
  }

  def main(args: Array[String]): Unit = {
    val driver = new M.MesosExecutorDriver(CommandExecutor)
    driver.run()
  }
}
