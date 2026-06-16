#include <Servo.h>
#include <SoftwareSerial.h>

// Bluetooth
// HC-05 / HC-06 TXD → Arduino pin 10
// HC-05 / HC-06 RXD → Arduino pin 11
SoftwareSerial bluetooth(10, 11);

Servo miServo;

const int PIN_SERVO = 9;

const int SERVO_MIN = 30;
const int SERVO_MAX = 150;
const int SERVO_CENTRO = 90;

// false = modo manual con app Bluetooth
// true  = modo automático con Python/OpenCV
bool modoAutomatico = false;

// ---------------- CONTROL ----------------

int destinoFinal = SERVO_CENTRO;
int objetivoSuave = SERVO_CENTRO;
int posActual = SERVO_CENTRO;
int ultimoAnguloEscrito = SERVO_CENTRO;

// ---------------- ANTI-VIBRACIÓN ----------------

// Si llega un comando con diferencia muy pequeña, se ignora
const int UMBRAL_NUEVO_COMANDO = 2;

// Zona muerta cerca del destino
const int ZONA_MUERTA = 2;

// Movimiento de 1 grado por paso
const int PASO_SERVO = 1;

// Más alto = más lento y suave
const unsigned long TIEMPO_PASO = 70;

// Tiempo detenido antes de desactivar el servo
const unsigned long TIEMPO_PARA_DETACH = 800;

// Si vibra mucho detenido, dejar en true
// Si la cámara se cae o pierde posición, poner false
const bool USAR_DETACH_AL_DETENER = true;

bool servoActivo = true;
bool estaMoviendo = false;

unsigned long tiempoPasoAnterior = 0;
unsigned long tiempoDetenido = 0;

void setup() {
  Serial.begin(9600);       // Python/OpenCV por USB
  bluetooth.begin(9600);    // App Android por Bluetooth

  Serial.setTimeout(5);
  bluetooth.setTimeout(5);

  miServo.attach(PIN_SERVO);
  miServo.write(SERVO_CENTRO);

  destinoFinal = SERVO_CENTRO;
  objetivoSuave = SERVO_CENTRO;
  posActual = SERVO_CENTRO;
  ultimoAnguloEscrito = SERVO_CENTRO;

  Serial.println("Sistema listo");
  Serial.println("Modo inicial: MANUAL");
}

void loop() {
  leerBluetooth();
  leerPython();
  moverServoPorRampa();
}

// ---------------- LECTURA BLUETOOTH ----------------

void leerBluetooth() {
  if (bluetooth.available() > 0) {
    String comandoBT = bluetooth.readStringUntil('\n');
    comandoBT.trim();

    if (comandoBT.length() > 0) {
      procesarBluetooth(comandoBT);
    }
  }
}

// ---------------- LECTURA PYTHON ----------------

void leerPython() {
  if (Serial.available() > 0) {
    String comandoPython = Serial.readStringUntil('\n');
    comandoPython.trim();

    if (comandoPython.length() > 0) {
      procesarPython(comandoPython);
    }
  }
}

// ---------------- PROCESAR BLUETOOTH ----------------

void procesarBluetooth(String comando) {
  if (comando == "M0") {
    modoAutomatico = false;

    destinoFinal = posActual;
    objetivoSuave = posActual;

    activarServo();

    Serial.println("MODO MANUAL ACTIVADO");
    bluetooth.println("MODO MANUAL ACTIVADO");
  }

  else if (comando == "M1") {
    modoAutomatico = true;

    destinoFinal = posActual;
    objetivoSuave = posActual;

    activarServo();

    Serial.println("MODO AUTOMATICO ACTIVADO");
    bluetooth.println("MODO AUTOMATICO ACTIVADO");
  }

  else if (esComandoAngulo(comando)) {
    if (modoAutomatico == false) {
      recibirDestino(comando);
    } else {
      Serial.println("APP ignorada: modo AUTOMATICO activo");
    }
  }
}

// ---------------- PROCESAR PYTHON ----------------

void procesarPython(String comando) {
  if (esComandoAngulo(comando)) {
    if (modoAutomatico == true) {
      recibirDestino(comando);
    } else {
      Serial.println("PYTHON ignorado: modo MANUAL activo");
    }
  }
}

// ---------------- VALIDAR COMANDO ----------------

bool esComandoAngulo(String comando) {
  if (comando.length() != 4) return false;
  if (comando[0] != 'A') return false;

  return isDigit(comando[1]) && isDigit(comando[2]) && isDigit(comando[3]);
}

// ---------------- RECIBIR DESTINO ----------------

void recibirDestino(String comando) {
  int nuevoDestino = comando.substring(1).toInt();
  nuevoDestino = constrain(nuevoDestino, SERVO_MIN, SERVO_MAX);

  // Ignorar comandos demasiado cercanos para evitar vibración
  if (abs(nuevoDestino - destinoFinal) < UMBRAL_NUEVO_COMANDO) {
    return;
  }

  destinoFinal = nuevoDestino;
  activarServo();

  Serial.print("Nuevo destino: ");
  Serial.println(destinoFinal);
}

// ---------------- MOVIMIENTO SUAVE ----------------

void moverServoPorRampa() {
  unsigned long tiempoActual = millis();

  if (tiempoActual - tiempoPasoAnterior >= TIEMPO_PASO) {
    tiempoPasoAnterior = tiempoActual;

    // Si ya está cerca del destino, se considera detenido
    if (abs(posActual - destinoFinal) <= ZONA_MUERTA) {
      estaMoviendo = false;
      posActual = destinoFinal;

      if (USAR_DETACH_AL_DETENER) {
        if (tiempoDetenido == 0) {
          tiempoDetenido = tiempoActual;
        }

        if (tiempoActual - tiempoDetenido >= TIEMPO_PARA_DETACH) {
          desactivarServo();
        }
      }

      return;
    }

    // Si hay movimiento, reiniciar tiempo detenido
    estaMoviendo = true;
    tiempoDetenido = 0;
    activarServo();

    if (posActual < destinoFinal) {
      posActual += PASO_SERVO;
    }

    else if (posActual > destinoFinal) {
      posActual -= PASO_SERVO;
    }

    posActual = constrain(posActual, SERVO_MIN, SERVO_MAX);

    if (abs(posActual - ultimoAnguloEscrito) >= 1) {
      miServo.write(posActual);
      ultimoAnguloEscrito = posActual;
    }
  }
}

// ---------------- ACTIVAR / DESACTIVAR SERVO ----------------

void activarServo() {
  if (!servoActivo) {
    miServo.attach(PIN_SERVO);
    miServo.write(posActual);
    delay(10);
    servoActivo = true;
  }
}

void desactivarServo() {
  if (servoActivo) {
    miServo.detach();
    servoActivo = false;
  }
}