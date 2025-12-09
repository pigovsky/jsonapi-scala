i wrote a function `com.qvantel.jsonapi.PersonDeduplicator$.deduplicateIncludes` in @core/src/test/scala/com/qvantel/jsonapi/PersonDeduplicator.scala

i test it using @core/src/test/scala/com/qvantel/jsonapi/DeduplicateIncludesSpec.scala

please create an universal implementation of this `deduplicateIncludes` function that:

* forms a set of includes `Set[((String, String), Any)]` recursively going through all loaded `ToOne.Loaded` and `ToMany.Loaded` fields of a given case class, where `(String, String)` is computed using `com.qvantel.jsonapi.Deduplicator.idType` (@core/src/main/scala/com/qvantel/jsonapi/Deduplicator.scala);
* groups the found set of includes by id `(String, String)` receiving `Map[(String, String), Set[Any]]`
* transforms `Map[(String, String), Set[Any]]` to the map `Map[(String, String), Any]` selecting the most defined `Any` from `Set[Any]`, the metric of most defined is `com.qvantel.jsonapi.Deduplicator.rank`
* recursively replaces all the `ToOne` and `ToMany` fields of the original case class with the most defined ones.