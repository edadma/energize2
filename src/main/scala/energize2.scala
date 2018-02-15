package xyz.hyperreal

import java.io.ByteArrayOutputStream
import java.time.{Instant, ZoneOffset}
import java.util.Base64

import scala.util.parsing.input.Position
import com.typesafe.config.ConfigFactory
import xyz.hyperreal.bvm.{AST, ExpressionAST, VM}


package object energize2 {

	type OBJ = Map[String, AnyRef]

	lazy val VERSION = "1.0-α1"
	lazy val CONFIG = ConfigFactory.load
	lazy val DATABASE = CONFIG.getConfig( "database" )
	lazy val SERVER = CONFIG.getConfig( "server" )
	lazy val AUTHORIZATION = CONFIG.getConfig( "authorization" )
	lazy val DATETIME = CONFIG.getConfig( "datetime" )
	lazy val ADMIN = CONFIG.getConfig( "admin" )

	private val hex = "0123456789ABCDEF"

	implicit def vm2proc( vm: VM ): Processor = vm.args.asInstanceOf[Processor]

	def now = Instant.now.atOffset( ZoneOffset.UTC )

	def nameIn( n: String ) = n + '_'

	def nameOut( n: String ) = n.substring( 0, n.length - 1 )

	def idIn = nameIn( "_id" )

	def escapeQuotes( s: String ): String = s replace ("'", "''")

	def escapeQuotes( json: OBJ ): OBJ =
		json map {case (k, v) =>
			(k, v match {
				case s: String => escapeQuotes( s )
				case _ => v
			})
		}

	def parseExpression( expression: String ): ExpressionAST = {
		val p = new EnergizeParser

		p.parseFromString( expression, p.expressionStatement )
	}

	//	def parseStatements( statements: String ) = {
	//		val p = new EnergizeParser
	//
	//		p.parseFromString( statements, p.statements )
	//	}

	def hex2array( s: String ) = s grouped 2 map (Integer.parseInt(_, 16).toByte) toList

	def byte2hex( b: Byte ) = new String( Array(hex charAt ((b&0xFF) >> 4), hex charAt (b&0x0F)) )

	def bytes2hex( a: Array[Byte] ) = a map byte2hex mkString

	def bytes2base64( data: Array[Byte] ) = new String( Base64.getMimeEncoder.encode(data) )

	def base642bytes( data: String ) = Base64.getMimeDecoder.decode( data.getBytes )

	def problem( pos: Position, error: String ) =
		if (pos eq null)
			sys.error( error )
		else
			sys.error( pos.line + ": " + error + "\n" + pos.longString )

	def run( program: String, args: Any* ): Unit = {
		val parser = new EnergizeParser
		val ast = parser.parseFromString( program, parser.source ).asInstanceOf[AST]
		val code = new EnergizeCompiler().compile( ast, H2Database, false )
		val vm = new VM( code, Array(), false, false, args )

		vm.execute
	}

	def runCapture( program: String ): String = {
		val outCapture = new ByteArrayOutputStream

		Console.withOut(outCapture) {
			run( program )
		}

		outCapture.toString.trim
	}

	def integer( s: String ) = s.forall( _.isDigit )

}

//def problem( pos: Int, input: ParserInput, msg: String ): Unit = {
//	val p = Position( pos, input )
//	val pe = ParseError( p, p, null )
//	import p._
//	val ef = new ErrorFormatter( expandTabs = 4 )
//	val (expandedCol, _) = ef.expandErrorLineTabs( input getLine line, column )
//
//	println( s"$msg (line $line, column $expandedCol):" )
//	println( ef.formatErrorLine( pe, input ) )
//}