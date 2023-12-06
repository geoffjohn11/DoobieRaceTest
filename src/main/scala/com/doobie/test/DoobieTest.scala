package com.doobie.test

import cats.effect.{IO, IOApp}
import doobie.implicits._
import doobie.{ConnectionIO, Transactor}

object DoobieTest extends IOApp.Simple {

  import DoobieTestImplicits._

  val xa: Transactor[IO] = Transactor
    .fromDriverManager[IO]("org.postgresql.Driver", "jdbc:postgresql:bank", "docker", "docker")

  case class Account(id: Long, balance: BigDecimal)

  def find(id: Long): ConnectionIO[Option[Account]] =
    sql"""
    SELECT
      id,
      balance
     FROM accounts
     WHERE id = $id
     """.query[Account].option

  def update(id: Long, balance: BigDecimal): ConnectionIO[Int] =
    sql"""
      UPDATE accounts
      SET
        balance = ${balance}
      WHERE id = ${id}
    """.update.run

    val id       = 44L
    val thousand = 1000L

    //find and update operation is executed as a single transaction
    def findAndUpdate() = (for {
      acc  <- find(id).debug
      _    <- acc.fold(update(id, BigDecimal(thousand)))(bal => update(id, bal.balance - thousand))
      accountMinus <- find(id).debug
      _    <- accountMinus.fold(update(id, BigDecimal(thousand)))(bal => update(id, bal.balance + thousand))
      accountPlus <- find(id).debug
    } yield accountPlus).transact(xa)

  override def run: IO[Unit] = {
    //always returns an account with balance 300 from left or right raced IO
    IO.race(findAndUpdate(), findAndUpdate()).flatMap {
      case Left(r)  => IO(s"Account Left is ${r}").debugIO.void
      case Right(l) => IO(s"Account Right is ${l}").debugIO.void
    }
  }
}

object DoobieTestImplicits {
  implicit class Debugger[A](io: ConnectionIO[A]) {
    def debug: ConnectionIO[A] = io.map { value =>
      println(s"[${Thread.currentThread().getName}] $value")
      value
    }
  }

  implicit class DebuggerIO[A](io: IO[A]) {
    def debugIO: IO[A] = io.map { value =>
      println(s"[${Thread.currentThread().getName}] $value")
      value
    }
  }

}
