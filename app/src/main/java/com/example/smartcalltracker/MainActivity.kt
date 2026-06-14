package com.example.smartcalltracker

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartcalltracker.ui.theme.SmartCallTrackerTheme
import kotlinx.coroutines.delay
import java.io.OutputStream
import java.util.UUID

class MainActivity : ComponentActivity() {

    private var bluetoothSocket: BluetoothSocket? = null
    private var salidaBluetooth: OutputStream? = null

    private val permisoBluetooth = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { permisoConcedido ->
        if (!permisoConcedido) {
            Toast.makeText(this, "Permiso Bluetooth rechazado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permisoBluetooth.launch(Manifest.permission.BLUETOOTH_CONNECT)
        }

        setContent {
            SmartCallTrackerTheme {
                SmartCallScreen(
                    conectarBluetooth = { cambiarEstado ->
                        conectarBluetooth(cambiarEstado)
                    },
                    desconectarBluetooth = { cambiarEstado ->
                        desconectarBluetooth(cambiarEstado)
                    },
                    enviarBluetooth = { comando ->
                        enviarBluetooth(comando)
                    }
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun conectarBluetooth(cambiarEstado: (String) -> Unit) {
        try {
            val adaptador = BluetoothAdapter.getDefaultAdapter()

            if (adaptador == null) {
                Toast.makeText(this, "Este celular no tiene Bluetooth", Toast.LENGTH_SHORT).show()
                return
            }

            if (!adaptador.isEnabled) {
                Toast.makeText(this, "Activa el Bluetooth del celular", Toast.LENGTH_LONG).show()
                return
            }

            val dispositivo = adaptador.bondedDevices.firstOrNull {
                it.name.equals("CHECO1", ignoreCase = true)
            }

            if (dispositivo == null) {
                Toast.makeText(this, "No encontré CHECO1 emparejado", Toast.LENGTH_LONG).show()
                return
            }

            val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

            adaptador.cancelDiscovery()

            bluetoothSocket = dispositivo.createRfcommSocketToServiceRecord(uuid)
            bluetoothSocket?.connect()

            salidaBluetooth = bluetoothSocket?.outputStream

            cambiarEstado("CONECTADO")
            Toast.makeText(this, "Conectado a CHECO1", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            cambiarEstado("DESCONECTADO")
            Toast.makeText(this, "Error al conectar con CHECO1", Toast.LENGTH_LONG).show()
        }
    }

    private fun desconectarBluetooth(cambiarEstado: (String) -> Unit) {
        try {
            salidaBluetooth?.close()
            bluetoothSocket?.close()

            salidaBluetooth = null
            bluetoothSocket = null

            cambiarEstado("DESCONECTADO")
            Toast.makeText(this, "Bluetooth desconectado", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(this, "Error al desconectar", Toast.LENGTH_SHORT).show()
        }
    }

    private fun enviarBluetooth(comando: String) {
        try {
            salidaBluetooth?.write(comando.toByteArray())
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo enviar comando", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun SmartCallScreen(
    conectarBluetooth: ((String) -> Unit) -> Unit,
    desconectarBluetooth: ((String) -> Unit) -> Unit,
    enviarBluetooth: (String) -> Unit
) {
    var estadoConexion by remember { mutableStateOf("DESCONECTADO") }
    var ultimoComando by remember { mutableStateOf("NINGUNO") }
    var modo by remember { mutableStateOf("MANUAL") }
    var anguloServo by remember { mutableFloatStateOf(90f) }
    var regresarCentro by remember { mutableStateOf(false) }

    val pasoServo = 1f
    val velocidadServo = 250L

    val botonIzquierda = remember { MutableInteractionSource() }
    val botonDerecha = remember { MutableInteractionSource() }

    val izquierdaPresionada by botonIzquierda.collectIsPressedAsState()
    val derechaPresionada by botonDerecha.collectIsPressedAsState()

    val fondo = Color(0xFF0F172A)
    val tarjeta = Color(0xFF1E293B)
    val azul = Color(0xFF2563EB)
    val verde = Color(0xFF22C55E)
    val rojo = Color(0xFFEF4444)
    val naranja = Color(0xFFF97316)
    val morado = Color(0xFF7C3AED)
    val textoBlanco = Color(0xFFF8FAFC)
    val textoGris = Color(0xFF94A3B8)

    fun moverServo(nuevoAngulo: Float) {
        anguloServo = nuevoAngulo.coerceIn(30f, 150f)
        ultimoComando = "A${anguloServo.toInt().toString().padStart(3, '0')}"

        if (estadoConexion == "CONECTADO" && modo == "MANUAL") {
            enviarBluetooth("$ultimoComando\n")
        }
    }

    LaunchedEffect(izquierdaPresionada, modo) {
        if (izquierdaPresionada && modo == "MANUAL") {
            regresarCentro = false
        }

        while (izquierdaPresionada && modo == "MANUAL") {
            moverServo(anguloServo - pasoServo)
            delay(velocidadServo)
        }
    }

    LaunchedEffect(derechaPresionada, modo) {
        if (derechaPresionada && modo == "MANUAL") {
            regresarCentro = false
        }

        while (derechaPresionada && modo == "MANUAL") {
            moverServo(anguloServo + pasoServo)
            delay(velocidadServo)
        }
    }

    LaunchedEffect(regresarCentro, modo) {
        while (regresarCentro && modo == "MANUAL" && anguloServo.toInt() != 90) {
            if (anguloServo > 90f) {
                moverServo(anguloServo - pasoServo)
            } else if (anguloServo < 90f) {
                moverServo(anguloServo + pasoServo)
            }

            delay(velocidadServo)
        }

        if (regresarCentro && modo == "MANUAL") {
            moverServo(90f)
            regresarCentro = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(fondo)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "SMART CALL",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = textoBlanco,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 18.dp)
        )

        Text(
            text = "Cámara inteligente por Bluetooth",
            fontSize = 15.sp,
            color = textoGris,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 22.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = tarjeta),
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ESTADO",
                    fontSize = 13.sp,
                    color = textoGris,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = estadoConexion,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (estadoConexion == "CONECTADO") verde else rojo,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "MODO: $modo",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (modo == "MANUAL") morado else naranja
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = tarjeta),
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ÁNGULO DEL SERVO",
                    fontSize = 13.sp,
                    color = textoGris,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "${anguloServo.toInt()}°",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = textoBlanco,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Slider(
                    value = anguloServo,
                    onValueChange = {
                        if (modo == "MANUAL") {
                            regresarCentro = false
                            moverServo(it)
                        }
                    },
                    valueRange = 30f..150f,
                    enabled = modo == "MANUAL",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = {
                if (modo == "MANUAL") {
                    regresarCentro = true
                }
            },
            enabled = modo == "MANUAL",
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = azul),
            modifier = Modifier
                .fillMaxWidth(0.62f)
                .height(58.dp)
        ) {
            Text(
                text = "CENTRO",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Button(
                onClick = {
                    if (modo == "MANUAL") {
                        regresarCentro = false
                        moverServo(anguloServo - pasoServo)
                    }
                },
                enabled = modo == "MANUAL",
                interactionSource = botonIzquierda,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = azul),
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp)
            ) {
                Text(
                    text = "IZQUIERDA",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = {
                    if (modo == "MANUAL") {
                        regresarCentro = false
                        moverServo(anguloServo + pasoServo)
                    }
                },
                enabled = modo == "MANUAL",
                interactionSource = botonDerecha,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = azul),
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp)
            ) {
                Text(
                    text = "DERECHA",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Button(
                onClick = {
                    modo = "MANUAL"
                    regresarCentro = false
                    ultimoComando = "M0"

                    if (estadoConexion == "CONECTADO") {
                        enviarBluetooth("M0\n")
                    }
                },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = morado),
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp)
            ) {
                Text(
                    text = "MANUAL",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = {
                    modo = "AUTOMÁTICO"
                    regresarCentro = false
                    ultimoComando = "M1"

                    if (estadoConexion == "CONECTADO") {
                        enviarBluetooth("M1\n")
                    }
                },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = naranja),
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp)
            ) {
                Text(
                    text = "AUTOMÁTICO",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = {
                if (estadoConexion == "DESCONECTADO") {
                    conectarBluetooth { nuevoEstado ->
                        estadoConexion = nuevoEstado
                    }
                } else {
                    desconectarBluetooth { nuevoEstado ->
                        estadoConexion = nuevoEstado
                    }
                }
            },
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (estadoConexion == "CONECTADO") rojo else verde
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
        ) {
            Text(
                text = if (estadoConexion == "DESCONECTADO") {
                    "CONECTAR A CHECO1"
                } else {
                    "DESCONECTAR DE CHECO1"
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ÚLTIMO COMANDO",
                    fontSize = 12.sp,
                    color = textoGris,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = ultimoComando,
                    fontSize = 18.sp,
                    color = textoBlanco,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}