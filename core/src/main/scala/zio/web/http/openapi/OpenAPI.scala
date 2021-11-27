package zio.web.http.openapi

import java.net.URI
import zio.NonEmptyChunk
import zio.web.docs.Doc
import zio.web.http.model.StatusCode

import scala.util.matching.Regex

/**
 * OpenAPI definitions based on https://spec.openapis.org/oas/v3.0.3
 * Other useful references:
 *   * https://swagger.io/specification/
 *   * https://github.com/NumberFour/openapi-scala
 *   * https://github.com/drwpow/openapi-typescript/blob/main/src/types.ts
 */
object OpenAPI {

  /**
   * This is the root document object of the OpenAPI document.
   *
   * @param openapi This string MUST be the semantic version number of the OpenAPI Specification version that the OpenAPI document uses. The openapi field SHOULD be used by tooling specifications and clients to interpret the OpenAPI document. This is not related to the API info.version string.
   * @param info Provides metadata about the API. The metadata MAY be used by tooling as required.
   * @param paths The available paths and operations for the API.
   * @param servers A List of Server Objects, which provide connectivity information to a target server. If the servers property is empty, the default value would be a Server Object with a url value of /.
   * @param components An element to hold various schemas for the specification.
   * @param security A declaration of which security mechanisms can be used across the API. The list of values includes alternative security requirement objects that can be used. Only one of the security requirement objects need to be satisfied to authorize a request. Individual operations can override this definition. To make security optional, an empty security requirement ({}) can be included in the List.
   * @param tags A list of tags used by the specification with additional metadata. The order of the tags can be used to reflect on their order by the parsing tools. Not all tags that are used by the Operation Object must be declared. The tags that are not declared MAY be organized randomly or based on the tools’ logic. Each tag name in the list MUST be unique.
   * @param externalDocs Additional external documentation.
   */
  final case class OpenAPI(
    openapi: String,
    info: Info,
    paths: Paths,
    servers: Option[List[Server]] = None,
    components: Option[Components] = None,
    security: Option[List[SecurityRequirement]] = None,
    tags: Option[List[Tag]] = None,
    externalDocs: Option[ExternalDocs] = None
  )

  /**
   * The object provides metadata about the API. The metadata MAY be used by the clients if needed, and MAY be presented in editing or documentation generation tools for convenience.
   *
   * @param title The title of the API.
   * @param version The version of the OpenAPI document (which is distinct from the OpenAPI Specification version or the API implementation version).
   * @param description A short description of the API.
   * @param termsOfService A URL to the Terms of Service for the API.
   * @param contact The contact information for the exposed API.
   * @param license The license information for the exposed API.
   */
  final case class Info(
    title: String,
    version: String,
    description: Option[Doc] = None,
    termsOfService: Option[URI] = None,
    contact: Option[Contact] = None,
    license: Option[License] = None
  )

  /**
   * Contact information for the exposed API.
   *
   * @param name The identifying name of the contact person/organization.
   * @param url The URL pointing to the contact information.
   * @param email The email address of the contact person/organization. MUST be in the format of an email address.
   */
  final case class Contact(name: Option[String] = None, url: Option[URI] = None, email: Option[String] = None)

  /**
   * License information for the exposed API.
   *
   * @param name The license name used for the API.
   * @param url A URL to the license used for the API.
   */
  final case class License(name: String, url: Option[URI] = None)

  /**
   * An object representing a Server.
   *
   * @param url A URL to the target host. This URL supports Server Variables and MAY be relative, to indicate that the host location is relative to the location where the OpenAPI document is being served. Variable substitutions will be made when a variable is named in {brackets}.
   * @param description Describing the host designated by the URL.
   * @param variables A map between a variable name and its value. The value is used for substitution in the server’s URL template.
   */
  final case class Server(
    url: URI,
    description: Option[Doc] = None,
    variables: Option[Map[String, ServerVariable]] = None
  )

  /**
   * An object representing a Server Variable for server URL template substitution.
   *
   * @param default The default value to use for substitution, which SHALL be sent if an alternate value is not supplied. Note this behavior is different than the Schema Object’s treatment of default values, because in those cases parameter values are optional. If the enum is defined, the value SHOULD exist in the enum’s values.
   * @param enum An enumeration of string values to be used if the substitution options are from a limited set.
   * @param description A description for the server variable.
   */
  final case class ServerVariable(
    default: String,
    enum: Option[NonEmptyChunk[String]] = None,
    description: Option[Doc] = None
  )

  /**
   * Holds a set of reusable objects for different aspects of the OAS. All objects defined within the components object will have no effect on the API unless they are explicitly referenced from properties outside the components object.
   *
   * @param schemas An object to hold reusable Schema Objects.
   * @param responses An object to hold reusable Response Objects.
   * @param parameters An object to hold reusable Parameter Objects.
   * @param examples An object to hold reusable Example Objects.
   * @param requestBodies An object to hold reusable Request Body Objects.
   * @param headers An object to hold reusable Header Objects.
   * @param securitySchemes An object to hold reusable Security Scheme Objects.
   * @param links An object to hold reusable Link Objects.
   * @param callbacks An object to hold reusable Callback Objects.
   */
  final case class Components(
    schemas: Option[Map[Key, SchemaOrReference]] = None,
    responses: Option[Map[Key, ResponseOrReference]] = None,
    parameters: Option[Map[Key, ParameterOrReference]] = None,
    examples: Option[Map[Key, ExampleOrReference]] = None,
    requestBodies: Option[Map[Key, RequestBodyOrReference]] = None,
    headers: Option[Map[Key, HeaderOrReference]] = None,
    securitySchemes: Option[Map[Key, SecuritySchemeOrReference]] = None,
    links: Option[Map[Key, LinkOrReference]] = None,
    callbacks: Option[Map[Key, CallbackOrReference]] = None
  )

  sealed abstract case class Key private (name: String)

  object Key {

    /**
     * All Components objects MUST use Keys that match the regular expression.
     */
    val validName: Regex = "^[a-zA-Z0-9.\\-_]+$.".r

    def fromString(name: String): Option[Key] = name match {
      case validName() => Some(new Key(name) {})
      case _           => None
    }
  }

  /**
   * Holds the relative paths to the individual endpoints and their operations. The path is appended to the URL from the Server Object in order to construct the full URL. The Paths MAY be empty, due to ACL constraints.
   */
  type Paths = Map[Path, PathItem]

  /**
   * The path is appended (no relative URL resolution) to the expanded URL from the Server Object's url field in order to construct the full URL. Path templating is allowed. When matching URLs, concrete (non-templated) paths would be matched before their templated counterparts. Templated paths with the same hierarchy but different templated names MUST NOT exist as they are identical. In case of ambiguous matching, it’s up to the tooling to decide which one to use.
   *
   * @param name The field name of the relative path MUST begin with a forward slash (/).
   */
  sealed abstract case class Path private (name: String)

  object Path {
    val validPath: Regex = "^/[a-zA-Z0-9.\\-_]+$.".r

    def fromString(name: String): Option[Path] = name match {
      case validPath() => Some(new Path(name) {})
      case _           => None
    }
  }

  /**
   * Describes the operations available on a single path. A Path Item MAY be empty, due to ACL constraints. The path itself is still exposed to the documentation viewer but they will not know which operations and parameters are available.
   *
   * @param ref Allows for an external definition of this path item. The referenced structure MUST be in the format of a Path Item Object. In case a Path Item Object field appears both in the defined object and the referenced object, the behavior is undefined.
   * @param summary An optional, string summary, intended to apply to all operations in this path.
   * @param description An optional, string description, intended to apply to all operations in this path. CommonMark syntax MAY be used for rich text representation.
   * @param get A definition of a GET operation on this path.
   * @param put A definition of a PUT operation on this path.
   * @param post A definition of a POST operation on this path.
   * @param delete A definition of a DELETE operation on this path.
   * @param options A definition of a OPTIONS operation on this path.
   * @param head A definition of a HEAD operation on this path.
   * @param patch A definition of a PATCH operation on this path.
   * @param trace A definition of a TRACE operation on this path.
   * @param servers An alternative server List to service all operations in this path.
   * @param parameters A Set of parameters that are applicable for all the operations described under this path. These parameters can be overridden at the operation level, but cannot be removed there. The Set can use the Reference Object to link to parameters that are defined at the OpenAPI Object’s components/parameters.
   */
  final case class PathItem(
    ref: Option[String] = None,
    summary: Option[String] = None,
    description: Option[Doc] = None,
    get: Option[Operation] = None,
    put: Option[Operation] = None,
    post: Option[Operation] = None,
    delete: Option[Operation] = None,
    options: Option[Operation] = None,
    head: Option[Operation] = None,
    patch: Option[Operation] = None,
    trace: Option[Operation] = None,
    servers: Option[List[Server]] = None,
    parameters: Option[Set[ParameterOrReference]] = None
  )

  /**
   * Describes a single API operation on a path.
   *
   * @param operationId Unique string used to identify the operation. The id MUST be unique among all operations described in the API. The operationId value is case-sensitive. Tools and libraries MAY use the operationId to uniquely identify an operation, therefore, it is RECOMMENDED to follow common programming naming conventions.
   *                    NOTE: spec does not say this is required, but it's best practice to include it
   * @param responses The List of possible responses as they are returned from executing this operation.
   * @param tags A list of tags for API documentation control. Tags can be used for logical grouping of operations by resources or any other qualifier.
   * @param summary A short summary of what the operation does.
   * @param description A verbose explanation of the operation behavior.
   * @param externalDocs Additional external documentation for this operation.
   * @param parameters A List of parameters that are applicable for this operation. If a parameter is already defined at the Path Item, the new definition will override it but can never remove it. The list MUST NOT include duplicated parameters. A unique parameter is defined by a combination of a name and location. The list can use the Reference Object to link to parameters that are defined at the OpenAPI Object’s components/parameters.
   * @param requestBody The request body applicable for this operation. The requestBody is only supported in HTTP methods where the HTTP 1.1 specification [RFC7231] has explicitly defined semantics for request bodies. In other cases where the HTTP spec is vague, requestBody SHALL be ignored by consumers.
   * @param callbacks A map of possible out-of band callbacks related to the parent operation. The key is a unique identifier for the Callback Object. Each value in the map is a Callback Object that describes a request that may be initiated by the API provider and the expected responses.
   * @param deprecated Declares this operation to be deprecated. Consumers SHOULD refrain from usage of the declared operation.
   * @param security A declaration of which security mechanisms can be used for this operation. The List of values includes alternative security requirement objects that can be used. Only one of the security requirement objects need to be satisfied to authorize a request. To make security optional, an empty security requirement ({}) can be included in the array. This definition overrides any declared top-level security. To remove a top-level security declaration, an empty List can be used.
   * @param servers An alternative server List to service this operation. If an alternative server object is specified at the Path Item Object or Root level, it will be overridden by this value.
   */
  final case class Operation(
    operationId: String,
    responses: Responses,
    tags: Option[List[String]] = None,
    summary: Option[String] = None,
    description: Option[Doc] = None,
    externalDocs: Option[ExternalDocs] = None,
    parameters: Option[Set[ParameterOrReference]] = None,
    requestBody: Option[RequestBodyOrReference] = None,
    callbacks: Option[Map[String, CallbackOrReference]] = None,
    deprecated: Option[Boolean] = None,
    security: Option[List[SecurityRequirement]] = None,
    servers: Option[List[Server]] = None
  )

  sealed trait ParameterOrReference

  /**
   * Allows referencing an external resource for extended documentation.
   *
   * @param url The URL for the target documentation. Value MUST be in the format of a URL.
   * @param description A short description of the target documentation. CommonMark syntax MAY be used for rich text representation.
   */
  case class ExternalDocs(
    url: URI,
    description: Option[Doc] = None
  )

  /**
   * Parameter Locations
   *
   * There are four possible parameter locations specified by the in field:
   * path - Used together with Path Templating, where the parameter value is actually part of the operation's URL. This does not include the host or base path of the API. For example, in /items/{itemId}, the path parameter is itemId.
   * query - Parameters that are appended to the URL. For example, in /items?id=###, the query parameter is id.
   * header - Custom headers that are expected as part of the request. Note that RFC7230 states header names are case insensitive.
   * cookie - Used to pass a specific cookie value to the API.
   */
  sealed trait ParameterLocation

  object ParameterLocation {
    final case object Query  extends ParameterLocation
    final case object Path   extends ParameterLocation
    final case object Header extends ParameterLocation
    final case object Cookie extends ParameterLocation
  }

  sealed trait Style

  object Style {
    final case object Matrix         extends Style
    final case object Label          extends Style
    final case object Simple         extends Style
    final case object Form           extends Style
    final case object SpaceDelimited extends Style
    final case object PipeDelimited  extends Style
    final case object DeepObject     extends Style
  }

  /**
   *
   * @param name The name of the parameter. Parameter names are case sensitive.
   *             If in is "path", the name field MUST correspond to a template expression occurring within the path field in the Paths Object. See Path Templating for further information.
   *             If in is "header" and the name field is "Accept", "Content-Type" or "Authorization", the parameter definition SHALL be ignored.
   *             For all other cases, the name corresponds to the parameter name used by the in property.
   * @param in The location of the parameter. Possible values are "query", "header", "path" or "cookie".
   * @param description A brief description of the parameter. This could contain examples of use. CommonMark syntax MAY be used for rich text representation.
   * @param required Determines whether this parameter is mandatory. If the parameter location is "path", this property is REQUIRED and its value MUST be true. Otherwise, the property MAY be included and its default value is false.
   * @param deprecated Specifies that a parameter is deprecated and SHOULD be transitioned out of usage. Default value is false.
   * @param allowEmptyValue Sets the ability to pass empty-valued parameters. This is valid only for query parameters and allows sending a parameter with an empty value. Default value is false. If style is used, and if behavior is n/a (cannot be serialized), the value of allowEmptyValue SHALL be ignored. Use of this property is NOT RECOMMENDED, as it is likely to be removed in a later revision.
   * @param style Describes how the parameter value will be serialized depending on the type of the parameter value. Default values (based on value of in): for query - form; for path - simple; for header - simple; for cookie - form.
   * @param explode When this is true, parameter values of type array or object generate separate parameters for each value of the array or key-value pair of the map. For other types of parameters this property has no effect. When style is form, the default value is true. For all other styles, the default value is false.
   * @param allowReserved Determines whether the parameter value SHOULD allow reserved characters, as defined by RFC3986 :/?#[]@!$&'()*+,;= to be included without percent-encoding. This property only applies to parameters with an in value of query. The default value is false.
   * @param schema The schema defining the type used for the parameter.
   * @param example Example of the parameter's potential value. The example SHOULD match the specified schema and encoding properties if present. The example field is mutually exclusive of the examples field. Furthermore, if referencing a schema that contains an example, the example value SHALL override the example provided by the schema. To represent examples of media types that cannot naturally be represented in JSON or YAML, a string value can contain the example with escaping where necessary.
   * @param examples Examples of the parameter's potential value. Each example SHOULD contain a value in the correct format as specified in the parameter encoding. The examples field is mutually exclusive of the example field. Furthermore, if referencing a schema that contains an example, the examples value SHALL override the example provided by the schema.
   */
  case class Parameter(
    name: String,
    in: ParameterLocation,
    description: Option[Doc] = None,
    required: Option[Boolean] = None,
    deprecated: Option[Boolean] = None,
    allowEmptyValue: Option[Boolean] = None,
    style: Option[Style] = None,
    explode: Option[Boolean] = None,
    allowReserved: Option[Boolean] = None,
    schema: Option[SchemaOrReference] = None,
    example: Option[Any] = None,
    examples: Option[Map[Key, ExampleOrReference]] = None,
    content: Option[Map[String, MediaType]] = None
  ) extends ParameterOrReference

  sealed trait RequestBodyOrReference

  /**
   * Describes a single request body.
   *
   * @param content The content of the request body. The key is a media type or [media type range]appendix-D) and the value describes it. For requests that match multiple keys, only the most specific key is applicable.
   * @param description A brief description of the request body. This could contain examples of use.
   * @param required Determines if the request body is required in the request.
   */
  final case class RequestBody(
    content: Map[String, MediaType],
    description: Option[Doc] = None,
    required: Option[Boolean] = None
  ) extends RequestBodyOrReference

  /**
   * Each Media Type Object provides schema and examples for the media type identified by its key.
   *
   * @param schema The schema defining the content of the request, response, or parameter.
   *               NOTE: spec does not say this is required, but it's best practice to include it
   * @param example Example of the media type. The example object SHOULD be in the correct format as specified by the media type. The example field is mutually exclusive of the examples field. Furthermore, if referencing a schema which contains an example, the example value SHALL override the example provided by the schema.
   * @param examples Examples of the media type. Each example object SHOULD match the media type and specified schema if present. If referencing a schema which contains an example, the examples value SHALL override the example provided by the schema.
   * @param encoding A map between a property name and its encoding information. The key, being the property name, MUST exist in the schema as a property. The encoding object SHALL only apply to requestBody objects when the media type is multipart or application/x-www-form-urlencoded.
   */
  final case class MediaType(
    schema: Option[SchemaOrReference],
    example: Option[Any] = None,
    examples: Option[Map[String, ExampleOrReference]] = None,
    encoding: Option[Map[String, Encoding]] = None
  )

  /**
   * A single encoding definition applied to a single schema property.
   *
   * @param contentType The Content-Type for encoding a specific property.
   * @param headers A map allowing additional information to be provided as headers, for example Content-Disposition. Content-Type is described separately and SHALL be ignored in this section. This property SHALL be ignored if the request body media type is not a multipart.
   * @param style Describes how a specific property value will be serialized depending on its type. This property SHALL be ignored if the request body media type is not application/x-www-form-urlencoded.
   * @param explode When this is true, property values of type array or object generate separate parameters for each value of the array, or key-value-pair of the map.
   * @param allowReserved Determines whether the parameter value SHOULD allow reserved characters, as defined by [RFC3986] to be included without percent-encoding. This property SHALL be ignored if the request body media type is not application/x-www-form-urlencoded.
   */
  final case class Encoding(
    contentType: Option[String] = None,
    headers: Option[Map[String, HeaderOrReference]],
    style: Option[String] = None,
    explode: Option[Boolean] = None,
    allowReserved: Option[Boolean] = None
  )

  /**
   * A container for the expected responses of an operation. The container maps a HTTP response code to the expected response.
   * The Responses Object MUST contain at least one response code, and it SHOULD be the response for a successful operation call.
   */
  type Responses = Map[StatusCode, ResponseOrReference]

  sealed trait ResponseOrReference

  /**
   * Describes a single response from an API Operation, including design-time, static links to operations based on the response.
   *
   * @param description A short description of the response.
   * @param headers Maps a header name to its definition. [RFC7230] states header names are case insensitive. If a response header is defined with the name "Content-Type", it SHALL be ignored.
   * @param content A map containing descriptions of potential response payloads. The key is a media type or [media type range]appendix-D) and the value describes it. For responses that match multiple keys, only the most specific key is applicable.
   * @param links A map of operations links that can be followed from the response. The key of the map is a short name for the link, following the naming constraints of the names for Component Objects.
   */
  final case class Response(
    description: Doc,
    headers: Option[Map[String, HeaderOrReference]] = None,
    content: Option[Map[String, MediaType]] = None,
    links: Option[Map[String, LinkOrReference]] = None
  ) extends ResponseOrReference

  sealed trait CallbackOrReference

  /**
   * A map of possible out-of band callbacks related to the parent operation. Each value in the map is a Path Item Object that describes a set of requests that may be initiated by the API provider and the expected responses. The key value used to identify the path item object is an expression, evaluated at runtime, that identifies a URL to use for the callback operation.
   */
  type Callback = Map[String, PathItem] with CallbackOrReference

  sealed trait ExampleOrReference

  /**
   * In all cases, the example value is expected to be compatible with the type schema of its associated value. Tooling implementations MAY choose to validate compatibility automatically, and reject the example value(s) if incompatible.
   *
   * @param summary Short description for the example.
   * @param description Long description for the example.
   * @param value Embedded literal example. The value field and externalValue field are mutually exclusive. To represent examples of media types that cannot naturally represented in JSON or YAML, use a string value to contain the example, escaping where necessary.
   * @param externalValue A URL that points to the literal example. This provides the capability to reference examples that cannot easily be included in JSON or YAML documents.
   */
  final case class Example(
    summary: Option[String] = None,
    description: Option[Doc] = None,
    value: Option[Any] = None,
    externalValue: Option[URI] = None
  ) extends ExampleOrReference

  sealed trait LinkOrReference

  /**
   * The Link object represents a possible design-time link for a response. The presence of a link does not guarantee the caller’s ability to successfully invoke it, rather it provides a known relationship and traversal mechanism between responses and other operations.
   *
   * Unlike dynamic links (i.e. links provided in the response payload), the OAS linking mechanism does not require link information in the runtime response.
   *
   * For computing links, and providing instructions to execute them, a runtime expression is used for accessing values in an operation and using them as parameters while invoking the linked operation.
   *
   * @param operationRef A relative or absolute URI reference to an OAS operation. This field is mutually exclusive of the operationId field, and MUST point to an Operation Object. Relative operationRef values MAY be used to locate an existing Operation Object in the OpenAPI definition.
   * @param operationId The name of an existing, resolvable OAS operation, as defined with a unique operationId. This field is mutually exclusive of the operationRef field.
   * @param parameters A map representing parameters to pass to an operation as identified via operationRef. The key is the parameter name to be used, whereas the value can be a constant or an expression to be evaluated and passed to the linked operation. The parameter name can be qualified using the parameter location [{in}.]{name} for operations that use the same parameter name in different locations (e.g. path.id).
   * @param requestBody A literal value or {expression} to use as a request body when calling the target operation.
   * @param description A description of the link.
   * @param server A server object to be used by the target operation.
   */
  final case class Link(
    operationRef: Option[URI] = None,
    operationId: Option[String] = None,
    parameters: Option[Map[String, Any]] = None,
    requestBody: Option[Any] = None,
    description: Option[Doc] = None,
    server: Option[Server] = None
  ) extends LinkOrReference

  sealed trait HeaderOrReference

  /**
   * The Header Object follows the structure of the Parameter Object with the following changes:
   * 1. name MUST NOT be specified, it is given in the corresponding headers map.
   * 2. in MUST NOT be specified, it is implicitly in header.
   * 3. All traits that are affected by the location MUST be applicable to a location of header (for example, style).
   *
   * @param description A brief description of the parameter. This could contain examples of use. CommonMark syntax MAY be used for rich text representation.
   * @param required Determines whether this parameter is mandatory. If the parameter location is "path", this property is REQUIRED and its value MUST be true. Otherwise, the property MAY be included and its default value is false.
   * @param deprecated Specifies that a parameter is deprecated and SHOULD be transitioned out of usage. Default value is false.
   * @param allowEmptyValue Sets the ability to pass empty-valued parameters. This is valid only for query parameters and allows sending a parameter with an empty value. Default value is false. If style is used, and if behavior is n/a (cannot be serialized), the value of allowEmptyValue SHALL be ignored. Use of this property is NOT RECOMMENDED, as it is likely to be removed in a later revision.
   * @param style Describes how the parameter value will be serialized depending on the type of the parameter value. Default values (based on value of in): for query - form; for path - simple; for header - simple; for cookie - form.
   * @param explode When this is true, parameter values of type array or object generate separate parameters for each value of the array or key-value pair of the map. For other types of parameters this property has no effect. When style is form, the default value is true. For all other styles, the default value is false.
   * @param allowReserved Determines whether the parameter value SHOULD allow reserved characters, as defined by RFC3986 :/?#[]@!$&'()*+,;= to be included without percent-encoding. This property only applies to parameters with an in value of query. The default value is false.
   * @param schema The schema defining the type used for the parameter.
   * @param example Example of the parameter's potential value. The example SHOULD match the specified schema and encoding properties if present. The example field is mutually exclusive of the examples field. Furthermore, if referencing a schema that contains an example, the example value SHALL override the example provided by the schema. To represent examples of media types that cannot naturally be represented in JSON or YAML, a string value can contain the example with escaping where necessary.
   * @param examples Examples of the parameter's potential value. Each example SHOULD contain a value in the correct format as specified in the parameter encoding. The examples field is mutually exclusive of the example field. Furthermore, if referencing a schema that contains an example, the examples value SHALL override the example provided by the schema.
   */
  final case class Header(
    description: Option[Doc] = None,
    required: Option[Boolean] = None,
    deprecated: Option[Boolean] = None,
    allowEmptyValue: Option[Boolean] = None,
    style: Option[Style] = None,
    explode: Option[Boolean] = None,
    allowReserved: Option[Boolean] = None,
    schema: Option[SchemaOrReference] = None,
    example: Option[Any] = None,
    examples: Option[Map[Key, ExampleOrReference]] = None,
    content: Option[Map[String, MediaType]] = None
  ) extends HeaderOrReference

  /**
   * Adds metadata to a single tag that is used by the Operation Object. It is not mandatory to have a Tag Object per tag defined in the Operation Object instances.
   *
   * @param name The name of the tag.
   * @param description A short description for the tag.
   * @param externalDocs Additional external documentation for this tag.
   */
  final case class Tag(name: String, description: Option[Doc] = None, externalDocs: Option[ExternalDocs] = None)

  /**
   * A simple object to allow referencing other components in the specification, internally and externally.
   *
   * @param ref The reference string.
   */
  final case class Reference(ref: String)
      extends SchemaOrReference
      with ResponseOrReference
      with ParameterOrReference
      with ExampleOrReference
      with RequestBodyOrReference
      with HeaderOrReference
      with SecuritySchemeOrReference
      with LinkOrReference
      with CallbackOrReference

  sealed trait InstanceType

  object InstanceType {
    final case object Object  extends InstanceType
    final case object Array   extends InstanceType
    final case object String  extends InstanceType
    final case object Number  extends InstanceType
    final case object Boolean extends InstanceType
    final case object Null    extends InstanceType
  }

  sealed trait SchemaOrReference

  /**
   * The Schema Object allows the definition of input and output data types. These types can be objects, but also
   * primitives and arrays. This object is an extended subset of the JSON Schema Specification Wright Draft 00.
   *
   * For more information about the properties, see JSON Schema Core and JSON Schema Validation. Unless stated
   * otherwise, the property definitions follow the JSON Schema.
   *
   * See docs for field definitions: https://spec.openapis.org/oas/v3.0.3#schema-object
   */
  final case class Schema(
    `type`: InstanceType,
    title: Option[String] = None,
    multipleOf: Option[Int] = None,
    maximum: Option[Int] = None,
    exclusiveMaximum: Option[Boolean] = None,
    minimum: Option[Int] = None,
    exclusiveMinimum: Option[Boolean] = None,
    maxLength: Option[Int] = None,
    minLength: Option[Int] = None,
    pattern: Option[String] = None,
    maxItems: Option[Int] = None,
    minItems: Option[Int] = None,
    uniqueItems: Option[Boolean] = None,
    maxProperties: Option[Int] = None,
    minProperties: Option[Int] = None,
    required: Option[List[String]] = None,
    enum: Option[List[String]] = None,
    allOf: Option[NonEmptyChunk[SchemaOrReference]] = None,
    oneOf: Option[NonEmptyChunk[SchemaOrReference]] = None,
    anyOf: Option[NonEmptyChunk[SchemaOrReference]] = None,
    not: Option[SchemaOrReference] = None,
    items: Option[SchemaOrReference] = None,
    properties: Option[Map[String, SchemaOrReference]] = None,
    additionalProperties: Option[SchemaOrReference] = None, // TODO: this can be a boolean too
    description: Option[Doc] = None,
    format: Option[String] = None,
    default: Option[Any] = None,
    nullable: Option[Boolean] = None,
    discriminator: Option[Discriminator] = None,
    readOnly: Option[Boolean] = None,
    writeOnly: Option[Boolean] = None,
    xml: Option[XML] = None,
    externalDocs: Option[ExternalDocs] = None,
    example: Option[String] = None,
    deprecated: Option[Boolean] = None
  ) extends SchemaOrReference

  /**
   * When request bodies or response payloads may be one of a number of different schemas, a discriminator object can be used to aid in serialization, deserialization, and validation. The discriminator is a specific object in a schema which is used to inform the consumer of the specification of an alternative schema based on the value associated with it.
   *
   * When using the discriminator, inline schemas will not be considered.
   *
   * @param propertyName The name of the property in the payload that will hold the discriminator value.
   * @param mapping An object to hold mappings between payload values and schema names or references.
   */
  final case class Discriminator(propertyName: String, mapping: Option[Map[String, String]] = None)

  /**
   * A metadata object that allows for more fine-tuned XML model definitions.
   *
   * When using arrays, XML element names are not inferred (for singular/plural forms) and the name property SHOULD be used to add that information.
   *
   * @param name Replaces the name of the element/attribute used for the described schema property. When defined within items, it will affect the name of the individual XML elements within the list. When defined alongside type being array (outside the items), it will affect the wrapping element and only if wrapped is true. If wrapped is false, it will be ignored.
   * @param namespace The URI of the namespace definition.
   * @param prefix The prefix to be used for the name.
   * @param attribute Declares whether the property definition translates to an attribute instead of an element.
   * @param wrapped MAY be used only for an array definition. Signifies whether the array is wrapped (for example, <books><book/><book/></books>) or unwrapped (<book/><book/>). The definition takes effect only when defined alongside type being array (outside the items).
   */
  final case class XML(
    name: Option[String] = None,
    namespace: Option[URI] = None,
    prefix: Option[String] = None,
    attribute: Option[Boolean] = None,
    wrapped: Option[Boolean] = None
  )

  sealed trait SecuritySchemeOrReference

  sealed trait SecurityScheme extends SecuritySchemeOrReference {
    def `type`: String
    def description: Doc
  }

  object SecurityScheme {

    /**
     * Defines an HTTP security scheme that can be used by the operations.
     *
     * @param description A short description for security scheme.
     * @param name The name of the header, query or cookie parameter to be used.
     * @param in The location of the API key.
     */
    final case class ApiKey(description: Doc, name: String, in: ApiKey.In) extends SecurityScheme {
      override def `type`: String = "apiKey"
    }

    object ApiKey {
      sealed trait In

      object In {
        case object Query  extends In
        case object Header extends In
        case object Cookie extends In
      }
    }

    /**
     *
     * @param description A short description for security scheme.
     * @param scheme The name of the HTTP Authorization scheme to be used in the Authorization header as defined in [RFC7235]. The values used SHOULD be registered in the IANA Authentication Scheme registry.
     * @param bearerFormat A hint to the client to identify how the bearer token is formatted. Bearer tokens are usually generated by an authorization server, so this information is primarily for documentation purposes.
     */
    final case class Http(description: Doc, scheme: String, bearerFormat: Option[String]) extends SecurityScheme {
      override def `type`: String = "http"
    }

    /**
     *
     * @param description A short description for security scheme.
     * @param flows An object containing configuration information for the flow types supported.
     */
    final case class OAuth2(description: Doc, flows: OAuthFlows) extends SecurityScheme {
      override def `type`: String = "oauth2"
    }

    /**
     *
     * @param description A short description for security scheme.
     * @param openIdConnectUrl OpenId Connect URL to discover OAuth2 configuration values.
     */
    final case class OpenIdConnect(description: Doc, openIdConnectUrl: URI) extends SecurityScheme {
      override def `type`: String = "openIdConnect"
    }
  }

  /**
   * Allows configuration of the supported OAuth Flows.
   *
   * @param `implicit` Configuration for the OAuth Implicit flow.
   * @param password Configuration for the OAuth Resource Owner Password flow
   * @param clientCredentials Configuration for the OAuth Client Credentials flow. Previously called application in OpenAPI 2.0.
   * @param authorizationCode Configuration for the OAuth Authorization Code flow. Previously called accessCode in OpenAPI 2.0.
   */
  final case class OAuthFlows(
    `implicit`: Option[OAuthFlow.Implicit],
    password: Option[OAuthFlow.Password],
    clientCredentials: Option[OAuthFlow.ClientCredentials],
    authorizationCode: Option[OAuthFlow.AuthorizationCode]
  )

  sealed trait OAuthFlow {
    def refreshUrl: Option[URI]
    def scopes: Map[String, String]
  }

  object OAuthFlow {

    /**
     * Configuration for the OAuth Implicit flow.
     *
     * @param authorizationUrl The authorization URL to be used for this flow.
     * @param refreshUrl The URL to be used for obtaining refresh tokens.
     * @param scopes The available scopes for the OAuth2 security scheme. A map between the scope name and a short description for it. The map MAY be empty.
     */
    final case class Implicit(authorizationUrl: URI, refreshUrl: Option[URI], scopes: Map[String, String])
        extends OAuthFlow

    /**
     * Configuration for the OAuth Authorization Code flow. Previously called accessCode in OpenAPI 2.0.
     *
     * @param authorizationUrl The authorization URL to be used for this flow.
     * @param refreshUrl The URL to be used for obtaining refresh tokens.
     * @param scopes The available scopes for the OAuth2 security scheme. A map between the scope name and a short description for it. The map MAY be empty.
     * @param tokenUrl The token URL to be used for this flow.
     */
    final case class AuthorizationCode(
      authorizationUrl: URI,
      refreshUrl: Option[URI],
      scopes: Map[String, String],
      tokenUrl: URI
    ) extends OAuthFlow

    /**
     * Configuration for the OAuth Resource Owner Password flow.
     *
     * @param refreshUrl The URL to be used for obtaining refresh tokens.
     * @param scopes The available scopes for the OAuth2 security scheme. A map between the scope name and a short description for it. The map MAY be empty.
     * @param tokenUrl The token URL to be used for this flow.
     */
    final case class Password(refreshUrl: Option[URI], scopes: Map[String, String], tokenUrl: URI) extends OAuthFlow

    /**
     * Configuration for the OAuth Client Credentials flow. Previously called application in OpenAPI 2.0.
     *
     * @param refreshUrl The URL to be used for obtaining refresh tokens.
     * @param scopes The available scopes for the OAuth2 security scheme. A map between the scope name and a short description for it. The map MAY be empty.
     * @param tokenUrl The token URL to be used for this flow.
     */
    final case class ClientCredentials(refreshUrl: Option[URI], scopes: Map[String, String], tokenUrl: URI)
        extends OAuthFlow
  }

  /**
   * Lists the required security schemes to execute this operation. The name used for each property MUST correspond to a security scheme declared in the Security Schemes under the Components Object.
   *
   * Security Requirement Objects that contain multiple schemes require that all schemes MUST be satisfied for a request to be authorized. This enables support for scenarios where multiple query parameters or HTTP headers are required to convey security information.
   *
   * When a list of Security Requirement Objects is defined on the OpenAPI Object or Operation Object, only one of the Security Requirement Objects in the list needs to be satisfied to authorize the request.
   */
  type SecurityRequirement = Map[String, List[String]]
}
