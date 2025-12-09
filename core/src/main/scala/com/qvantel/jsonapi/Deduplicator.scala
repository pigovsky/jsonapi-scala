package com.qvantel.jsonapi

import shapeless.syntax.std.tuple.productTupleOps
import spray.json.{JsArray, JsObject, JsString, JsValue}

import scala.collection.immutable.ListMap

object Deduplicator {

  def includedEntities(jsVal: JsValue): Vector[((String, String), JsValue)] = {
    jsVal.asJsObject.fields("included") match {
      case JsArray(elements) =>
        elements.map {
          e =>
            idType(e) -> e
        }
    }
  }

  def idType(e: JsValue): (String, String) = {
    val eType = e.asJsObject.fields("type") match {
      case JsString(v) => v
    }
    val id = e.asJsObject.fields("id") match {
      case JsString(v) => v
    }
    (eType, id)
  }

  implicit class JsObjectOps(jsObj: JsObject) {
    def patch(updates: (String, JsValue)*): JsObject = {
      JsObject(jsObj.fields ++ updates)
    }

    /**
     * Returns a new JsObject where fields are sorted alphabetically.
     * Uses ListMap to ensure that spray-json printers respect the sorted order.
     */
    def sorted: JsObject = {
      JsObject(ListMap(jsObj.fields.toSeq.sortBy(_._1): _*))
    }
  }

  def mergeJsObjs(one: JsObject, other: JsObject): JsObject = {
    JsObject(
      one.fields.map{
        case (key, value) =>
          key -> (other.fields.get(key) match {
            case Some(x) => merge(value, x)
            case _ => value
          })
      } ++ other.fields.filterNot{
        case (key, value) => one.fields.contains(key)
      }
    )
  }

  def mergeJsArrays(one: JsArray, other: JsArray): JsArray = {
    if (one.elements.length > other.elements.length) {
      one
    } else if (other.elements.length > one.elements.length) {
      other
    } else {
      JsArray(
        one.elements.zip(other.elements).map{
          case (x, y) => merge(x, y)
        }
      )
    }
  }

  def merge(one: JsValue, other: JsValue): JsValue = {
    one match {
      case jsObj: JsObject =>
        mergeJsObjs(jsObj, other.asJsObject)
      case jsArray: JsArray =>
        mergeJsArrays(jsArray, other.asInstanceOf[JsArray])
      case x => x
    }
  }

  def merge(objects: List[JsValue]): JsValue = {
    objects match {
      case head :: Nil =>
        head
      case head :: tail =>
        merge(head, merge(tail))
    }
  }

  def deduplicateIncludes(jsVal: JsValue): JsValue = {
    val idEntitiesSeq = includedEntities(jsVal)

    val entitiesGroupedById = idEntitiesSeq.groupBy(_._1)

    val duplicates = entitiesGroupedById.filter(_._2.length > 1)

    val deduplicated = duplicates.map(
      x => merge(x._2.map(_._2.asJsObject).toList)
    ).toVector ++ idEntitiesSeq.filterNot {
      case (key, value) => duplicates.contains(key)
    }.map(_._2)

    jsVal.asJsObject.patch(
      "included" -> JsArray(deduplicated)
    )
  }
}
