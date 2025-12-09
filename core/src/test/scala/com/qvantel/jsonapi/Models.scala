package com.qvantel.jsonapi

import _root_.spray.json.DefaultJsonProtocol._


object Models {
  implicit val apiRoot: ApiRoot = ApiRoot(None)

  @jsonApiResource final case class Task(id: String)

  @jsonApiResource final case class Person(
                                            id: String,
                                            children: ToMany[Person] = ToMany.reference,
                                            tasks: ToMany[Task] = ToMany.reference,
                                          )

  @jsonApiResource final case class Family(
                                            id: String,
                                            father: ToOne[Person],
                                            mother: ToOne[Person],
                                          )
}