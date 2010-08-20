package de.sciss.android.synth

import _root_.android.app.Activity
import _root_.android.content.Context
import _root_.android.net.wifi.WifiManager
import _root_.android.os.{ Bundle, Handler }
import _root_.android.view.View
import _root_.android.widget.{ Button, CompoundButton, EditText, ScrollView, TableLayout, TableRow, TextView,
   ToggleButton }
import de.sciss.osc.{OSCClient, OSCMessage, OSCTransmitter, UDP}
import java.net.{InetAddress, InetSocketAddress}
import de.sciss.synth
import synth.{ ClientOptionsBuilder, Model, Server, ServerConnection, ServerOptionsBuilder, Synth }

class MainActivity extends Activity {
   activity =>

   var trnsO: Option[ OSCTransmitter ] = None
   var clientO: Option[ OSCClient ] = None
   var serverConnectionO: Option[ ServerConnection ] = None
   var serverO: Option[ Server ] = None

   lazy val lbResult = new TextView( activity )
   val handler = new Handler()

   private val scListener : Model.Listener = {
      case ServerConnection.Running( s ) => {
         serverO = Some( s )
         defer { lbResult.setText( "Connected" )}
      }
      case ServerConnection.Aborted => {
         defer { lbResult.setText( "Aborted" )}
      }
   }

   private def defer( code: => Unit ) {
      handler.post( new Runnable { def run = code })
   }

   private def startServer( serverAddr: InetSocketAddress ) {
      val wifi    = getSystemService( Context.WIFI_SERVICE ).asInstanceOf[ WifiManager ]
      val dhcp    = wifi.getDhcpInfo()
      val myIP    = dhcp.ipAddress
      val myAddr  = InetAddress.getByAddress( Array(
         (myIP & 0xFF).toByte, ((myIP >> 8) & 0xFF).toByte, ((myIP >> 16) & 0xFF).toByte, ((myIP >> 24) & 0xFF).toByte ))
      val mySock  = new InetSocketAddress( myAddr, 0 )

      val so   = new ServerOptionsBuilder()
      val co   = new ClientOptionsBuilder()
      so.host  = serverAddr.getHostName()
      so.port  = serverAddr.getPort()
      co.addr  = Some( mySock ) 
      val sc   = Server.connect( "remote", so.build, co.build )
      serverConnectionO = Some( sc )
      sc.addListener( scListener )
      sc.start
   }

   override def onCreate( savedInstanceState: Bundle ) {
      super.onCreate( savedInstanceState )

      val scroll  = new ScrollView( activity )
      val table   = new TableLayout( activity )
      val row1    = new TableRow( activity )
      val lbSocket = new TextView( activity )
      lbSocket.setText( "Socket (host:port):")
      val ggSocket = new EditText( activity )
      ggSocket.setText( "192.168.0.100:57110" )
      row1.addView( lbSocket )
      row1.addView( ggSocket )
      table.addView( row1 )

      val row2    = new TableRow( activity )
      val lbServer   = new TextView( activity )
      lbServer.setText( "Server:" )
      val ggServer = new ToggleButton( activity )
      ggServer.setOnCheckedChangeListener( new CompoundButton.OnCheckedChangeListener() {
         def onCheckedChanged( view: CompoundButton, isChecked: Boolean ) = try {
            serverO foreach { s =>
               s.dispose
               serverO = None
            }
            serverConnectionO foreach { sc =>
               sc.removeListener( scListener )
               sc.abort
               serverConnectionO = None
            }
            if( isChecked ) {
               val socketStr = ggSocket.getText().toString()
               val i = socketStr.indexOf( ':' )
               val host = socketStr.substring( 0, i )
               val port = socketStr.substring( i + 1 ).toInt
               lbResult.setText( "Connecting..." )
               startServer( new InetSocketAddress( host, port ))
            }
         }
         catch {
            case e => {
               view.setChecked( false )
               lbResult.setText( e.getClass().getName() + " : " + e.getMessage() )
            }
         }
      })
      row2.addView( lbServer )
      row2.addView( ggServer )
      table.addView( row2 )
      
      val row3    = new TableRow( activity )
      val lbBubbles   = new TextView( activity )
      lbBubbles.setText( "Bubbles:" )
      val ggBubbles = new ToggleButton( activity )
      var synthO: Option[ Synth ] = None
      ggBubbles.setOnCheckedChangeListener( new CompoundButton.OnCheckedChangeListener() {
         def onCheckedChanged( view: CompoundButton, isChecked: Boolean ) = serverO foreach { s => try {
            import synth._
            import synth.ugen._
            synthO foreach { synth =>
               synth.release( 4 )
               synthO = None
            }
            if( isChecked ) {
               val synth = play {
                  val f = LFSaw.kr( 0.4 ).madd( 24, LFSaw.kr( Seq( 8, 7.23 )).madd( 3, 80 )).midicps
                  CombN.ar( SinOsc.ar( f ) * 0.04, 0.2, 0.2, 4 )
               }
               synthO = Some( synth )
            }
         }
         catch {
            case e => {
               view.setChecked( false )
               lbResult.setText( e.getClass().getName() + " : " + e.getMessage() )
            }
         }}
      })
      row3.addView( lbBubbles )
      row3.addView( ggBubbles )
      table.addView( row3 )

      val row4  = new TableRow( activity )
      row4.addView( lbResult )
      table.addView( row4 )

      scroll.addView( table )
      setContentView( scroll )
   }

   def onCreateX( savedInstanceState: Bundle ) {
      super.onCreate( savedInstanceState )

      val wifi = getSystemService( Context.WIFI_SERVICE ).asInstanceOf[ WifiManager ]
      val dhcp = wifi.getDhcpInfo()
      val scroll  = new ScrollView( activity )
      val table   = new TableLayout( activity )
      val row1    = new TableRow( activity )
      val lbIP    = new TextView( activity )
      import dhcp._
      lbIP.setText( "" + dns1 + " " + dns2 + " " + gateway + " " + ipAddress + " " + netmask + " " + serverAddress )

      row1.addView( lbIP )
      table.addView( row1 )
      scroll.addView( table )
      setContentView( scroll )
   }

   def onCreateXY( savedInstanceState: Bundle ) {
      super.onCreate( savedInstanceState )

      val wifi    = getSystemService( Context.WIFI_SERVICE ).asInstanceOf[ WifiManager ]
      val dhcp    = wifi.getDhcpInfo()
      val myIP    = dhcp.ipAddress
      val myAddr  = InetAddress.getByAddress( Array(
         (myIP & 0xFF).toByte, ((myIP >> 8) & 0xFF).toByte, ((myIP >> 16) & 0xFF).toByte, ((myIP >> 24) & 0xFF).toByte ))
      val mySock  = new InetSocketAddress( myAddr, 0 )

      val scroll  = new ScrollView( activity )
//      LayoutUtils.Layout.WidthFill_HeightWrap.applyViewGroupParams( scroll )
      val table   = new TableLayout( activity )
//      LayoutUtils.Layout.WidthFill_HeightFill.applyViewGroupParams( table )
      val row1    = new TableRow( activity )
      val lbIP    = new TextView( activity )
      lbIP.setText( "IP" )
//      text.setPadding( RowPadding, RowPadding, RowPadding, RowPadding )
      val ggIP = new EditText( activity )
      ggIP.setText( "192.168.0.100")
//      button.setPadding( RowPadding, RowPadding, RowPadding, RowPadding )
//      button.setOnClickListener( new View.OnClickListener() {
//         def onClick( view: View ) {
//            count += 1
//            text.setText( "Clicked: " + count )
////            activity.runFadeOutAnimationAndFinish()
//         }
//      })
      row1.addView( lbIP )
      row1.addView( ggIP )
//      LayoutUtils.Layout.WidthWrap_HeightWrap.applyTableLayoutParams( row )
//      row.setPadding( RowPadding, RowPadding, RowPadding, RowPadding )
      table.addView( row1 )

      val row2    = new TableRow( activity )
      val lbPort  = new TextView( activity )
      lbPort.setText( "Port" )
      val ggPort  = new EditText( activity )
      ggPort.setText( "55555")
      row2.addView( lbPort )
      row2.addView( ggPort )
      table.addView( row2 )

      val row3    = new TableRow( activity )
      val lbMessage = new TextView( activity )
      lbMessage.setText( "Message" )
      val ggMessage  = new EditText( activity )
      ggMessage.setText( "\"/hallo\", \"welt\"")
      row3.addView( lbMessage )
      row3.addView( ggMessage )
      table.addView( row3 )

      val row4    = new TableRow( activity )
      val ggSend  = new Button( activity )
      ggSend.setText( "Send!" )
//      ggSend.setLayoutParams( new TableRow.LayoutParams( 2, 1 ))
      row4.addView( ggSend )
      table.addView( row4 )

      val row5    = new TableRow( activity )
      val lbResult   = new TextView( activity )
//      lbResult.setText( "Ready." )
//      lbResult.setLayoutParams( new TableRow.LayoutParams( 2, 1 ))
      row5.addView( lbResult )
      table.addView( row5 )

      scroll.addView( table )
      setContentView( scroll )

      ggSend.setOnClickListener( new View.OnClickListener() {
         def onClick( view: View ) = trnsO foreach { trns =>
//            if( !trns.isConnected ) trns.connect
            lbResult.setText( try {
               val host = ggIP.getText().toString()
               val port = ggPort.getText().toString().toInt
               val addr = new InetSocketAddress( host, port )
               val msga = ggMessage.getText().toString().split( ',' ) map { str0 =>
                  val str = str0.trim
                  if( str.length >= 2 && str.startsWith( "\"" ) && str.endsWith( "\"" )) {
                     str.substring( 1, str.length - 1 )
                  } else if( str.indexOf( '.' ) >= 0 ) {
                     str.toFloat
                  } else {
                     str.toInt
                  }
               }
               val Array( name: String, args @ _* ) = msga
               val msg  = OSCMessage( name, args: _* )
               trns.send( msg, addr )
               "Sent."
            }
            catch {
               case e => e.getMessage() // e.getClass().getName()
            })
         }
      })

//      val trns = OSCTransmitter( UDP )
      val trns = OSCTransmitter.withAddress( UDP, mySock )
      trnsO = Some( trns )
      trns.connect
      lbResult.setText( trns.localAddress.toString )
   }

   override def onDestroy {
      serverConnectionO foreach { sc =>
         sc.abort
         serverConnectionO = None
      }
      serverO foreach { s =>
         s.dispose
         serverO = None
      }
      clientO foreach { c =>
         c.dispose
         clientO = None
      }
      trnsO foreach { t =>
         t.dispose
         trnsO = None
      }
      super.onDestroy()
   }
}