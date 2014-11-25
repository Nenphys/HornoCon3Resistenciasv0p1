// Providerconf3.java del proyecto TermostatoV1p3: HMI del Controlador de Horno de Curado, EZ Air de Ing. Arana//
     // 5 cambios de Formulas m y b por cambio de signo, ECH, 22sep14//
package nebula.link.android;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.serialport.SerialPort;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.AbsoluteLayout;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;


@SuppressWarnings("deprecation")
public class Providerconf3 extends Activity {

	TextView RxESC;
	char toca = 0;
	
   

	/* Se llama PuertoD1 para hacer referencia a la nomenclatura del Pico-SAM */
	SerialPort				PuertoD1;
	OutputStream			EscribeNivelInferior;
	InputStream				LeeNivelInferior;
	InputStream				LeeNivelSuperior;
	ReadThreadNivelInferior	RecibeESC;
	ReadThreadNivelSuperior	RecibeServer;
	ImageView back;

	
	/* Prueba Modbus con parametros fijos */
	/** Comando para traer los primeros 8 registros*/
	//byte[] toSend 		= {0x16,0x03,0x00,0x00,0x00,0x08};
	/** comando lectura (3) incluye desde controles hasta analogicas*/
	//byte[] toSend 		= {0x16,0x03,0x00,0x02,0x00,0x10};
	
	/** control on/off de reles comando escritura (6) */
	
	//byte[] toSend 		= {0x16,0x06,0x00,0x02,0x00,(byte)0x80};
	
	byte[] toSendconCRC = new byte[256];
	

/* ================================================================================================== */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
  
        proceso = (TextView)findViewById(R.id.textView1);
        temperatura = (TextView)findViewById(R.id.textView2);
        tg= (ToggleButton) findViewById(R.id.toggleButton1);
        conf = (ImageView)findViewById(R.id.settings);
        Pantallaconf= (AbsoluteLayout)findViewById(R.id.layoutconf);
        PantallaPrincipal = (AbsoluteLayout)findViewById(R.id.absoluteLayout1);
        back= (ImageView)findViewById(R.id.back);
        GuardaMax =(ImageView)findViewById(R.id.savetempmax);
        GuardaMin= (ImageView)findViewById(R.id.savetempmin);
        GuardarTC =(ImageView)findViewById(R.id.savetiempocurado);
        //RxESC = (TextView) findViewById(R.id.RxESC);
        final TextView calibracion =(TextView)findViewById(R.id.textView9);
        final ImageView guardarcalibracion = (ImageView)findViewById(R.id.guardarcali);
        final AbsoluteLayout pantallaCalibracion = (AbsoluteLayout)findViewById(R.id.calibracion);
        pantallaCalibracion.setVisibility(View.INVISIBLE);
        final TextView mvieja= (TextView)findViewById(R.id.mvieja);
		final TextView bvieja= (TextView)findViewById(R.id.bvieja);
		final EditText m = (EditText)findViewById(R.id.m);
		final EditText b = (EditText)findViewById(R.id.b);
		final ImageView guardab = (ImageView)findViewById(R.id.guardab);
		final ImageView backcali = (ImageView)findViewById(R.id.backcali);
		
	
		
		
		
		backcali.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				pantallaCalibracion.setVisibility(View.INVISIBLE);
				Pantallaconf.setVisibility(View.VISIBLE);
				
			}
		});
		
		guardab.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (b.length()==0){
					return;
				}else{
				ContentValues valuesb = new ContentValues();
				double nvab = Double.valueOf(b.getText().toString());
				valuesb.put(ContentProviderLabinal.COMANDO,nvab);
			     	getContentResolver().update(
			        		Uri.parse("content://com.nebula.labinal/cmd/2"), valuesb,null, null);
			     	bvieja.setText(""+ConsultaContentProviderb());
			     	b.setText("");
				}
			}
		});
		
		
		
        calibracion.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				pantallaCalibracion.setVisibility(View.VISIBLE);
				Pantallaconf.setVisibility(View.INVISIBLE);
				mvieja.post(new Runnable() {
					
					@Override
					public void run() {
						mvieja.setText(""+ConsultaContentProviderm());
						bvieja.setText(""+ConsultaContentProviderb());
						
					}
				});
				
			}
		});
        guardarcalibracion.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (m.length()==0){
					return;
				}else{
				ContentValues valuesm = new ContentValues();
				double nvam = Double.valueOf(m.getText().toString());
		        valuesm.put(ContentProviderLabinal.COMANDO,nvam);
		        getContentResolver().update(
		        		Uri.parse("content://com.nebula.labinal/cmd/1"), valuesm,null, null);
		        mvieja.setText(""+ConsultaContentProviderm());
		        m.setText("");
				}
			}
		});


       PantallaPrincipal.setVisibility(View.INVISIBLE);




        try {
			ConfiguraPuertoSerial();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		
       
        EscribeNivelInferior = PuertoD1.getOutputStream();
		try {
			EscribeNivelInferior.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		LeeNivelInferior = PuertoD1.getInputStream();
		
/* ================================================================================================== */
		Timer ExploraESC = new Timer();
		ExploraESC.schedule(new TimerTask() {
			@Override
			public void run() {
				
				if (!ComandoAEnviarANivelInferior.equals("")){
					int idpuerto = RecuperaID(Uri.parse("content://com.nebula.labinal/puerto"));
					int idcmdexploracion = RecuperaID(Uri.parse("content://com.nebula.labinal/cmdexplora"));
				
					if(idpuerto!=0 && idcmdexploracion !=0){
						EnviaComandosESC();
						try {
							Thread.sleep(50);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}else{
						RxESC.post(new Runnable() {
							public void run() {
								RxESC.setText("Checar configuracion de tablas de puerto y de comandos de exploracion");
							}
						});
					
					}
				}
			}
		}, 0, 500);
		
		//Boton Prendido Apagado
		tg.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				 if (isChecked) {
					 ComandoAEnviarANivelInferior="";
						try {
							Thread.sleep(250);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					 ComandoAEnviarANivelInferior = "0x01,0x06,0x00,0x17,0xFF,0xFF";
					 
					
					 
			        } else {
			        	ComandoAEnviarANivelInferior="";
						try {
							Thread.sleep(250);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
			        	ComandoAEnviarANivelInferior = "0x01,0x06,0x00,0x17,0x00,0x00";
			           
			        }
				 
			}
		});
		//Boton Configuracion
		conf.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				CmdExpToca=2;
				Pantallaconf.setVisibility(View.VISIBLE);
				PantallaPrincipal.setVisibility(View.INVISIBLE);
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		//Boton back
		back.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				CmdExpToca=1;
				PantallaPrincipal.setVisibility(View.VISIBLE);
				Pantallaconf.setVisibility(View.INVISIBLE);
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		});
		//Boton Guarda Max
		GuardaMax.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				double m =ConsultaContentProviderm();
				double b = ConsultaContentProviderb();
				NvoMax= (EditText)findViewById(R.id.nvomax);
				if(NvoMax.length()==0){
					return;
				}else{
//                    int ctas = (int) ((Integer.valueOf(NvoMax.getText().toString())-b)/(-m));
					int ctas = (int) ((Integer.valueOf(NvoMax.getText().toString())+b)/(m));
					String comando = Integer.toHexString(ctas);
					Log.i("comando", ""+comando);
					ComandoAEnviarANivelInferior="";
					try {
						Thread.sleep(250);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if(comando.length()==1){
						Log.i("MAX 1CARACTER", ""+comando);
						ComandoAEnviarANivelInferior = "0x01,0x06,0x00,0x2A,0x00,0x0"+comando;
						
						NvoMax.setText("");
					}else if (comando.length()==2){
						Log.i("MAX 2CARACTER", ""+comando);
						ComandoAEnviarANivelInferior = "0x01,0x06,0x00,0x2A,0x00,0x"+comando;
						
						NvoMax.setText("");				
					}else if (comando.length()==3){
						Log.i("MAX 3CARACTER", ""+comando);
						ComandoAEnviarANivelInferior = "0x01,0x06,0x00,0x2A,0x0"+comando.substring(0,1)+",0x"+comando.substring(1,3);
						
						NvoMax.setText("");				
					}else if (comando.length()==4){
						Log.i("MAX 4CARACTER", ""+comando);
						ComandoAEnviarANivelInferior = "0x01,0x06,0x00,0x2A,0x"+comando.substring(0,2)+",0x"+comando.substring(2,4);
						
						NvoMax.setText("");				
					}else{
						return;
					}
				}
			}
		});
		
		//Boton Guarda Min
		GuardaMin.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				double m =ConsultaContentProviderm();
				double b = ConsultaContentProviderb();
				NvoMin= (EditText)findViewById(R.id.nvomin);
				if(NvoMin.length()==0){
					return;
				}else{
//                    int ctas = (int) ((Integer.valueOf(NvoMin.getText().toString())-b)/(-m));
					int ctas = (int) ((Integer.valueOf(NvoMin.getText().toString())+b)/(m));
					String comando = Integer.toHexString(ctas);
					ComandoAEnviarANivelInferior="";
					try {
						Thread.sleep(250);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if(comando.length()==1){
						Log.i("MAX 1CARACTER", ""+comando);
						ComandoAEnviarANivelInferior = "0x01,0x06,0x00,0x2B,0x00,0x0"+comando;
						
						NvoMin.setText("");
					}else if (comando.length()==2){
						Log.i("MAX 2CARACTER", ""+comando);
						ComandoAEnviarANivelInferior = "0x01,0x06,0x00,0x2B,0x00,0x"+comando;
						
						NvoMin.setText("");				
					}else if (comando.length()==3){
						Log.i("MAX 3CARACTER", ""+comando);
						ComandoAEnviarANivelInferior = "0x01,0x06,0x00,0x2B,0x0"+comando.substring(0,1)+",0x"+comando.substring(1,3);
						
						NvoMin.setText("");				
					}else if (comando.length()==4){
						Log.i("MAX 4CARACTER", ""+comando);
						ComandoAEnviarANivelInferior = "0x01,0x06,0x00,0x2B,0x"+comando.substring(0,2)+",0x"+comando.substring(2,4);
						
						NvoMin.setText("");				
					}else{
						return;
					}
				}
			}
		});
		// Boton Guardar Tiempo curado 
		GuardarTC.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				NvoTC= (EditText)findViewById(R.id.nvotiempocuradado);
				if(NvoTC.length()==0){
					return;
				}else{
					int cntasTC =(int) (65535-(Integer.valueOf(NvoTC.getText().toString())*60));
					String comando = Integer.toHexString(cntasTC);
					ComandoAEnviarANivelInferior="";
					try {
						Thread.sleep(250);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if(comando.length()==1){
						Log.i("MAX 1CARACTER", ""+comando);
						ComandoAEnviarANivelInferior = "0x01,0x06,0x00,0x3E,0x00,0x0"+comando;
						
						NvoTC.setText("");
					}else if (comando.length()==2){
						Log.i("MAX 2CARACTER", ""+comando);
						ComandoAEnviarANivelInferior = "0x01,0x06,0x00,0x3E,0x00,0x"+comando;
						
						NvoTC.setText("");				
					}else if (comando.length()==3){
						Log.i("MAX 3CARACTER", ""+comando);
						ComandoAEnviarANivelInferior = "0x01,0x06,0x00,0x3E,0x0"+comando.substring(0,1)+",0x"+comando.substring(1,3);
						
						NvoTC.setText("");				
					}else if (comando.length()==4){
						Log.i("MAX 4CARACTER", ""+comando);
						ComandoAEnviarANivelInferior = "0x01,0x06,0x00,0x3E,0x"+comando.substring(0,2)+",0x"+comando.substring(2,4);
						
						NvoTC.setText("");				
					}else{
						return;
					}
				}
				
			}
		});
		

		
/* ================================================================================================== */

		
		/* Crea threads de RX */
		RecibeESC = new ReadThreadNivelInferior();
		RecibeESC.start();
		RecibeServer = new ReadThreadNivelSuperior();
		RecibeServer.start();

    }//onCreate

/* ================================================================================================== */
	private void EnviaComandosESC() {
		int cambio = ConsultaContentProvider("cambio");
		if(cambio==1){
			PuertoD1.close();
			try {
				ConfiguraPuertoSerial();
			} catch (SecurityException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			 ContentValues values = new ContentValues();
		        values.put(ContentProviderLabinal.CAMBIO,0);
		        getContentResolver().update(
		    			Uri.parse("content://com.nebula.labinal/puerto/1"),
		    			values,null,null);
			
		}else {
			/** comando lectura (3) incluye desde controles hasta analogicas*/
			byte[] toSend = TablaComandos();
			try {
				EscribeNivelInferior.flush();
				System.arraycopy(toSend, 0, toSendconCRC, 0, toSend.length);
				System.arraycopy(CRC16IBM(toSend), 0, toSendconCRC, toSend.length, 2);
				EscribeNivelInferior.write(toSendconCRC,0,toSend.length+2);	
			} catch (IOException e) {
					e.printStackTrace();
			}
		}

	}
	
/* ================================================================================================== */
	private void ConfiguraPuertoSerial() throws SecurityException, IOException {
		int baudrate = ConsultaContentProvider("velocidad");
		PuertoD1 = new SerialPort(new File("/dev/ttyS1"),baudrate);
           	
	}

/* ================================================================================================== */
	private byte[] CRC16IBM (byte[] buffer) {
	
        int[] table = {
                0x0000, 0xC0C1, 0xC181, 0x0140, 0xC301, 0x03C0, 0x0280, 0xC241,
                0xC601, 0x06C0, 0x0780, 0xC741, 0x0500, 0xC5C1, 0xC481, 0x0440,
                0xCC01, 0x0CC0, 0x0D80, 0xCD41, 0x0F00, 0xCFC1, 0xCE81, 0x0E40,
                0x0A00, 0xCAC1, 0xCB81, 0x0B40, 0xC901, 0x09C0, 0x0880, 0xC841,
                0xD801, 0x18C0, 0x1980, 0xD941, 0x1B00, 0xDBC1, 0xDA81, 0x1A40,
                0x1E00, 0xDEC1, 0xDF81, 0x1F40, 0xDD01, 0x1DC0, 0x1C80, 0xDC41,
                0x1400, 0xD4C1, 0xD581, 0x1540, 0xD701, 0x17C0, 0x1680, 0xD641,
                0xD201, 0x12C0, 0x1380, 0xD341, 0x1100, 0xD1C1, 0xD081, 0x1040,
                0xF001, 0x30C0, 0x3180, 0xF141, 0x3300, 0xF3C1, 0xF281, 0x3240,
                0x3600, 0xF6C1, 0xF781, 0x3740, 0xF501, 0x35C0, 0x3480, 0xF441,
                0x3C00, 0xFCC1, 0xFD81, 0x3D40, 0xFF01, 0x3FC0, 0x3E80, 0xFE41,
                0xFA01, 0x3AC0, 0x3B80, 0xFB41, 0x3900, 0xF9C1, 0xF881, 0x3840,
                0x2800, 0xE8C1, 0xE981, 0x2940, 0xEB01, 0x2BC0, 0x2A80, 0xEA41,
                0xEE01, 0x2EC0, 0x2F80, 0xEF41, 0x2D00, 0xEDC1, 0xEC81, 0x2C40,
                0xE401, 0x24C0, 0x2580, 0xE541, 0x2700, 0xE7C1, 0xE681, 0x2640,
                0x2200, 0xE2C1, 0xE381, 0x2340, 0xE101, 0x21C0, 0x2080, 0xE041,
                0xA001, 0x60C0, 0x6180, 0xA141, 0x6300, 0xA3C1, 0xA281, 0x6240,
                0x6600, 0xA6C1, 0xA781, 0x6740, 0xA501, 0x65C0, 0x6480, 0xA441,
                0x6C00, 0xACC1, 0xAD81, 0x6D40, 0xAF01, 0x6FC0, 0x6E80, 0xAE41,
                0xAA01, 0x6AC0, 0x6B80, 0xAB41, 0x6900, 0xA9C1, 0xA881, 0x6840,
                0x7800, 0xB8C1, 0xB981, 0x7940, 0xBB01, 0x7BC0, 0x7A80, 0xBA41,
                0xBE01, 0x7EC0, 0x7F80, 0xBF41, 0x7D00, 0xBDC1, 0xBC81, 0x7C40,
                0xB401, 0x74C0, 0x7580, 0xB541, 0x7700, 0xB7C1, 0xB681, 0x7640,
                0x7200, 0xB2C1, 0xB381, 0x7340, 0xB101, 0x71C0, 0x7080, 0xB041,
                0x5000, 0x90C1, 0x9181, 0x5140, 0x9301, 0x53C0, 0x5280, 0x9241,
                0x9601, 0x56C0, 0x5780, 0x9741, 0x5500, 0x95C1, 0x9481, 0x5440,
                0x9C01, 0x5CC0, 0x5D80, 0x9D41, 0x5F00, 0x9FC1, 0x9E81, 0x5E40,
                0x5A00, 0x9AC1, 0x9B81, 0x5B40, 0x9901, 0x59C0, 0x5880, 0x9841,
                0x8801, 0x48C0, 0x4980, 0x8941, 0x4B00, 0x8BC1, 0x8A81, 0x4A40,
                0x4E00, 0x8EC1, 0x8F81, 0x4F40, 0x8D01, 0x4DC0, 0x4C80, 0x8C41,
                0x4400, 0x84C1, 0x8581, 0x4540, 0x8701, 0x47C0, 0x4680, 0x8641,
                0x8201, 0x42C0, 0x4380, 0x8341, 0x4100, 0x81C1, 0x8081, 0x4040,
            };

            int crc = 0xFFFF;
            for (byte b : buffer) {
              crc = (crc >>> 8) ^ table[(crc ^ b) & 0xff];
            }
        byte[] arrayCRC = new byte[2];
        arrayCRC [0] = (byte) (crc & 0xff);
        arrayCRC [1] = (byte) (crc >> 8 & 0xff);
		return(arrayCRC);
	}

/* ================================================================================================== */
	private boolean CRCValido (byte[] buffer){
		try {
			if (buffer.length > 2){
				/* Saca Mensaje de lo Rx */
				byte[] mensaje = new byte[buffer.length - 2];
				System.arraycopy(buffer,0,mensaje,0,buffer.length - 2);

				/* Calcula CRC */
				byte[] temp = CRC16IBM(mensaje);

				/* Saca CRC de lo Rx */
				byte[] CRC = new byte[2];
				System.arraycopy(buffer,buffer.length-2,CRC,0,2);

				/* Compara con lo Rx */
				return(Arrays.equals(CRC, temp));
			}else{
				return(false);
			}
		} catch (NegativeArraySizeException e) {
			e.printStackTrace();
			return(false);
		}
	}
	
/* ================================================================================================== */
	private class ReadThreadNivelInferior extends Thread {
		@Override
		public void run() {
			super.run();
			while(!isInterrupted()) {
				int size;
				try {
					byte[] buffer = new byte[256];
					if (LeeNivelInferior == null){	
					}else{
						size = LeeNivelInferior.read(buffer);
						try{
						/* Verifica CRC */
						byte[] tempCRC = new byte[size];
						System.arraycopy(buffer,0,tempCRC,0,size);
							if ( size > 0 ){
								if ( CRCValido(tempCRC)) {
									DatosRecibidos(buffer, size);
								}
							}
						}
						catch (NegativeArraySizeException e){
							Log.i(" (RxInf) ", "Error Arreglo");
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
					Log.i(" (RxInf) ", "Error IO");
				}
			}
		}
		
/* ================================================================================================== */
		private void DatosRecibidos(final byte[] buffer, final int size) {
				String tempFormato = null;
				temp = "";
				for (int i = 0; i<size; i++){
					tempFormato = String.format("%02X", buffer[i]);
					temp = temp.concat(tempFormato);
				}
				if(temp.substring(0,5).equals("01050")||temp.substring(0,5).equals("01060")){
					
					ComandoAEnviarANivelInferior ="39";
					
					
				}
				
				Log.i(" (RxInf) Respuesta Nivel Inferior",temp );
				String palabraControl = temp.substring(2, 4);
				// checamos por la configuracion de que es si es un 80 es un veris y si es un 81 seria un termo
				Uri Campos =Uri.parse("content://com.nebula.labinal/confg39/1");
				Cursor c = getContentResolver().query(Campos,null,null,null,null);
				c.moveToFirst();
				int TipoDispositivo =(c.getInt(c.getColumnIndex("idmensaje39")));
				c.close();
				int TipoRespuesta =0;
				try{
					TipoRespuesta = Integer.valueOf(palabraControl);
				}
				catch (Exception e) {
					TipoRespuesta =0;
				}
				
				switch (TipoDispositivo) {
				
					case 81:
						switch (TipoRespuesta) {
							case 3:
								//byte Estado =  (byte)(int)Integer.valueOf(temp.substring(12,14),16);
								Pantallaconf= (AbsoluteLayout)findViewById(R.id.layoutconf);
								final TextView cntas = (TextView)findViewById(R.id.textView8);
								final TextView Sp1 = (TextView)findViewById(R.id.cntascalibracion);
								final TextView cntassp2 = (TextView)findViewById(R.id.textView10);
								final TextView Sp2 = (TextView)findViewById(R.id.sp2);
								PantallaPrincipal = (AbsoluteLayout)findViewById(R.id.absoluteLayout1);
							    final TextView cntastemp =(TextView)findViewById(R.id.textView11);
							    final TextView tempcntas = (TextView)findViewById(R.id.cntastep);
							    PantallaPrincipal.setOnLongClickListener(new OnLongClickListener() {
							    	 
	
									@Override
									public boolean onLongClick(View v) {
										if((cntastemp.getVisibility()==View.VISIBLE)&& (tempcntas.getVisibility()==View.VISIBLE)){
													cntastemp.setVisibility(View.INVISIBLE);
													tempcntas.setVisibility(View.INVISIBLE);
													
												}else{
													cntastemp.setVisibility(View.VISIBLE);
													tempcntas.setVisibility(View.VISIBLE);
												}
										return false;
									}
								});
							    
							  
							   
							    
								
									/** Entradas digitales**/
									// Lector de Temperatura
							    	TCtranscurrido= (TextView)findViewById(R.id.TCtranscurrido);
									temperatura.post(new Runnable() {
										
										@Override
										public void run() {
											double m =ConsultaContentProviderm();
											double b = ConsultaContentProviderb();
											int ctas =0;
											try {
												 ctas = (int)Integer.valueOf(temp.substring(62,66),16);
											} catch (StringIndexOutOfBoundsException e) {
												ctas=0;
											}

//                                            double TempF= (ctas*(-m))+b;
											double TempF= (ctas*(m))-b;
											temperatura.setText(""+TempF);
											try{
												tempcntas.setText(""+Integer.valueOf(temp.substring(62, 66), 16));
											}catch (StringIndexOutOfBoundsException e) {
												
											}
											try{
//											double Tctrans =TiempototalDeCuraro-(double)((65535-Integer.valueOf(temp.substring(86,90),16))/60);
											float Tctrans =(float) (TiempototalDeCuraro-((65535-(float)Integer.valueOf(temp.substring(86,90),16))/60));
											DecimalFormat  decimal = new DecimalFormat("#.#");
                                            if (Tctrans <= 0){
                                                TCtranscurrido.setText("-- de "+totalminTC.getText());
                                            }else{
                                                TCtranscurrido.setText(""+decimal.format(Tctrans)+" de "+totalminTC.getText());
                                            }

											}catch (StringIndexOutOfBoundsException e) {
												// TODO: handle exception
											}
										}
									});
									//Lector de Alarma
									proceso.post(new Runnable() {
										
										@Override
										public void run() {
											byte Estado = 0;
											try {
												 Estado =  (byte)(int)Integer.valueOf(temp.substring(12,14),16);
											} catch (StringIndexOutOfBoundsException e) {
												// TODO: handle exception
											}
											
										
											if (EstadoBitED(Estado, 2)){
												proceso.setText(R.string.Alamarma);
												proceso.setVisibility(View.VISIBLE);
												
											}else {
												proceso.setVisibility(View.INVISIBLE);
											}
											
										}
									});		
									//Sensor Flujo Aire
									flechauno= (ImageView)findViewById(R.id.flechauno);
									flechados = (ImageView)findViewById(R.id.flechados);
									flechauno.post(new Runnable() {
										
										@Override
										public void run() {
											byte Estado = 0;
											try {
												 Estado =  (byte)(int)Integer.valueOf(temp.substring(12,14),16);
											} catch (StringIndexOutOfBoundsException e) {
												// TODO: handle exception
											}
											if(EstadoBitED(Estado, 3)){
												flechauno.setVisibility(View.VISIBLE);
												flechados.setVisibility(View.VISIBLE);
												Log.i("Sensor Flujo","Prendido");
											}
											else{
												byte EstadoSD =0;
												try{
													 EstadoSD =  (byte)(int)Integer.valueOf(temp.substring(8,10),16);
												}catch (StringIndexOutOfBoundsException e) {
													EstadoSD=0;
												}
												
												if(EstadoBitSD(EstadoSD, 1)){
													
													proceso.setText(R.string.AlarmaFan);
													
												}
												flechauno.setVisibility(View.INVISIBLE);
												flechados.setVisibility(View.INVISIBLE);
												Log.i("Sensor Flujo","Apagando");
											}			
											
										}
									});
									
									// Puerta 1
									Pcerradauno = (ImageView)findViewById(R.id.cerradauno);
									Pcerradados = (ImageView)findViewById(R.id.cerradados);
									Pabiertados = (ImageView)findViewById(R.id.abiertados);
									Pabiertauno = (ImageView)findViewById(R.id.abiertauno);
									Pcerradauno.post(new Runnable() {

										@Override
										public void run() {
											byte Estado = 0;
											try {
												 Estado =  (byte)(int)Integer.valueOf(temp.substring(12,14),16);
											} catch (StringIndexOutOfBoundsException e) {
												// TODO: handle exception
											}
											if(EstadoBitED(Estado, 4)){
												Pabiertauno.setVisibility(View.INVISIBLE);
												Pcerradauno.setVisibility(View.VISIBLE);
												Log.i("Puerta Cerrada","Puerta1 Cerrada");

											}
											else{
												Pcerradauno.setVisibility(View.INVISIBLE);
												Pabiertauno.setVisibility(View.VISIBLE);
												Log.i("Puerta Abierta","Puerta1 Abierta");

											}

										}
									});
									// Puerta 2
//									Pcerradados.post(new Runnable() {
//
//										@Override
//										public void run() {
//											byte Estado = 0;
//											try {
//												 Estado =  (byte)(int)Integer.valueOf(temp.substring(12,14),16);
//											} catch (StringIndexOutOfBoundsException e) {
//												// TODO: handle exception
//											}
//											if(EstadoBitED(Estado, 4)){
//												Pabiertados.setVisibility(View.INVISIBLE);
//												Pcerradados.setVisibility(View.VISIBLE);
//												Log.i("Puerta Cerrada","Puerta2 Cerrada");
//                                                ComandoAEnviarANivelInferior = "0x01,0x06,0x00,0x17,0xFF,0xFF";//todo quitar esto de aqui
//											}
//											else{
//												Pcerradados.setVisibility(View.INVISIBLE);
//												Pabiertados.setVisibility(View.VISIBLE);
//												Log.i("Puerta Abierta","Puerta2 Abierta");
//                                                ComandoAEnviarANivelInferior = "0x01,0x06,0x00,0x17,0x00,0x00";//todo quitar esto de aqui
//
//
//											}
//
//										}
//									});
									//paro emergencia
									final TextView paro =(TextView)findViewById(R.id.textView17);
									
									paro.post(new Runnable() {
										
										@Override
										public void run() {
											paroemergencia= (ImageView)findViewById(R.id.paroemergencia);
											byte Estado = 0;
											try {
												 Estado =  (byte)(int)Integer.valueOf(temp.substring(12,14),16);
											} catch (StringIndexOutOfBoundsException e) {
												// TODO: handle exception
											}
											if(EstadoBitED(Estado, 6)){
												paroemergencia.setVisibility(View.INVISIBLE);
												paro.setVisibility(View.INVISIBLE);
											}
											else{
												paroemergencia.setVisibility(View.VISIBLE);
												paro.setVisibility(View.VISIBLE);
												paro.setText(R.string.ParoEM);
												Log.i("Paro Emergencia","Paro Emergencia");
												
												
											}
											
										}
									});
									
									
									/**Salidas Digitales**/
									venton  = (ImageView)findViewById(R.id.venton);
									venton.post(new Runnable() {
										
										@Override
										public void run() {
											byte Estado=0;
											try{
											 Estado =  (byte)(int)Integer.valueOf(temp.substring(8,10),16);
											}catch (StringIndexOutOfBoundsException e) {
												// TODO: handle exception
											}
											if(EstadoBitSD(Estado, 1)){
												venton.setVisibility(View.VISIBLE);
												TCtranscurrido.setVisibility(View.VISIBLE);
												totalminTC.setVisibility(View.INVISIBLE);

												
											}else{
												venton.setVisibility(View.INVISIBLE);
												TCtranscurrido.setVisibility(View.INVISIBLE);
												totalminTC.setVisibility(View.VISIBLE);
											}
											
										}
									});
									//Resistencia
									reson = (ImageView)findViewById(R.id.resistencia);
                                    reson.post(new Runnable() {

                                        @Override
                                        public void run() {
                                            byte Estado=0;
                                            byte Estado2=0;
                                            byte Estado3=0;
                                            try{
                                                Estado =  (byte)(int)Integer.valueOf(temp.substring(8,10),16);
                                            }catch (StringIndexOutOfBoundsException e) {
                                            }
                                            if((EstadoBitSD(Estado, 2)) || (EstadoBitSD(Estado2, 3)|| (EstadoBitSD(Estado3, 4)))){
                                                //no hacer nada
                                            }else{
                                                    reson.setVisibility(View.INVISIBLE);
                                             }

                                            }
                                    });
                                    //Resistencia1
                                    focoreson1= (ImageView)findViewById(R.id.resitencia1);
                                    focoreson1.post(new Runnable() {
										
										@Override
										public void run() {
											byte Estado=0;
											try{
											 Estado =  (byte)(int)Integer.valueOf(temp.substring(8,10),16);
											}catch (StringIndexOutOfBoundsException e) {
												// TODO: handle exception
											}
											if(EstadoBitSD(Estado, 2)){
                                                focoreson1.setVisibility(View.VISIBLE);
                                                reson.setVisibility(View.VISIBLE);
											}else{
                                                focoreson1.setVisibility(View.INVISIBLE);
											}
											
										}
									});
                                //Resistencia2
                                reson2 = (ImageView)findViewById(R.id.resistencia2);
                                reson2.post(new Runnable() {

                                    @Override
                                    public void run() {
                                        byte Estado=0;
                                        try{
                                            Estado =  (byte)(int)Integer.valueOf(temp.substring(8,10),16);
                                        }catch (StringIndexOutOfBoundsException e) {
                                            // TODO: handle exception
                                        }
                                        if(EstadoBitSD(Estado, 3)){
                                            reson2.setVisibility(View.VISIBLE);
                                            reson.setVisibility(View.VISIBLE);
                                        }else{
                                            reson2.setVisibility(View.INVISIBLE);

                                        }

                                    }
                                });
                                //Resistencia3
                                reson3 = (ImageView)findViewById(R.id.resistencia3);
                                reson3.post(new Runnable() {

                                    @Override
                                    public void run() {
                                        byte Estado=0;
                                        try{
                                            Estado =  (byte)(int)Integer.valueOf(temp.substring(8,10),16);
                                        }catch (StringIndexOutOfBoundsException e) {
                                            // TODO: handle exception
                                        }
                                        if(EstadoBitSD(Estado, 4)){
                                            reson3.setVisibility(View.VISIBLE);
                                            reson.setVisibility(View.VISIBLE);
                                        }else{
                                            reson3.setVisibility(View.INVISIBLE);

                                        }

                                    }
                                });
									//Valores de setpoint MAX y MIN
									max= (TextView)findViewById(R.id.tempmax);
									min =(TextView)findViewById(R.id.tempmin);
									TC = (TextView)findViewById(R.id.tiempocurado);
									totalminTC = (TextView)findViewById(R.id.totalminTC);
									if(CmdExpToca==2){
									max.post(new Runnable() {
										
										@Override
										public void run() {
											double m =ConsultaContentProviderm();
											double b = ConsultaContentProviderb();
											int ctasMax =0;
											int TcViejo =0;
											try{
												ctasMax = (int)Integer.valueOf(temp.substring(6,10),16);
											}catch (StringIndexOutOfBoundsException e) {
												// TODO: handle exception
											}
//                                            TempMax = (int) ((ctasMax*(-m))+b);
											TempMax = (int) ((ctasMax*(m))-b);
											max.setText(""+TempMax +"°F");
											
											int ctasMin=0;
											try{
											ctasMin = (int)Integer.valueOf(temp.substring(10,14),16);
											}catch (StringIndexOutOfBoundsException e) {
												// TODO: handle exception
											}
//                                            TempMin= (int) ((ctasMin*(-m))+b);
											TempMin= (int) ((ctasMin*(m))-b);
											min.setText(""+TempMin+"°F");
											
											try{
												TcViejo = (int)Integer.valueOf(temp.substring(86,90),16);
											}catch (StringIndexOutOfBoundsException e) {
											
											}
												TiempototalDeCuraro=(65535-TcViejo)/60;
												TC.setText(""+TiempototalDeCuraro+" Min");
												totalminTC.setText(""+TiempototalDeCuraro+" Min");
											try{
											Sp1.setText(""+Integer.valueOf(temp.substring(6,10),16));
											Sp2.setText(""+Integer.valueOf(temp.substring(10,14),16));
											}catch (StringIndexOutOfBoundsException e) {
												// TODO: handle exception
											}
											
											
										}
									});
									
									// Calibracion 
									
									Pantallaconf.setOnLongClickListener(new OnLongClickListener() {
										
										@Override
										public boolean onLongClick(View v) {
											if((cntas.getVisibility()==View.VISIBLE)&& (Sp1.getVisibility()==View.VISIBLE)&&
											   (cntassp2.getVisibility()==View.VISIBLE)&& (Sp2.getVisibility()==View.VISIBLE)){
												cntas.setVisibility(View.INVISIBLE);
												Sp1.setVisibility(View.INVISIBLE);
												cntassp2.setVisibility(View.INVISIBLE);
												Sp2.setVisibility(View.INVISIBLE);
											}else{
												cntas.setVisibility(View.VISIBLE);
												Sp1.setVisibility(View.VISIBLE);
												cntassp2.setVisibility(View.VISIBLE);
												Sp2.setVisibility(View.VISIBLE);
											}
											
											return false;
										}
									});
									
									}
									
									
									ComandoNivelSuperior = Acomoda39Termos(temp);
									ComandoAEnviarANivelInferior= "39";
									Log.i(" (RxInf) Exploracion Correcta (39)",ComandoNivelSuperior );
								//}
								
								break;
							case 5:
								if(ComandoAEnviarANivelInferior.regionMatches(8, "5", 0, 1)){
									ComandoNivelSuperior = Acomoda35(temp);
									ComandoAEnviarANivelInferior="";
									Log.i(" (RxInf) Comando Correcto (35)",ComandoNivelSuperior);
								}
								break;
							case 6:
								if(ComandoAEnviarANivelInferior.regionMatches(8, "6", 0, 1)){
									ComandoNivelSuperior = Acomoda35(temp);
									ComandoAEnviarANivelInferior="";
									Log.i(" (RxInf) Comando Correcto (35)",ComandoNivelSuperior);
								}
								break;

							default:
								if(ComandoAEnviarANivelInferior.equals("39")){
									ComandoNivelSuperior = Acomoda99();
									Log.i(" (RxInf) Exploracion Incorrecta (99)",ComandoNivelSuperior);
								}else{
									ComandoNivelSuperior = Acomoda95();
									Log.i(" (RxInf) Comando Incorrecto (95)",ComandoNivelSuperior);
								}
							
								break;
						}
				}
			}
	}
	
/* ================================================================================================== */
	private class ReadThreadNivelSuperior extends Thread {
		@Override
		public void run() {
			super.run();

			while(!isInterrupted()) {
				if (LeeNivelSuperior == null){
				}else{
					byte[] temp1 = new byte[512];
					try {
						int size = LeeNivelSuperior.read(temp1);
						String toPrint = new String(temp1,0,40);
						String Error = new String(temp1,10,5);
						if (size > 0){
							if (!Error.equals("Error")){
								Log.i(" (RxSup) Comando Recibido",toPrint.substring(0,40));
								int res = Integer.valueOf(toPrint.substring(10,11));
								switch (res) {
									case 9:
										ComandoAEnviarANivelInferior = "39";
										break;
									case 5:
										ComandoAEnviarANivelInferior = toPrint.substring(11);
										break;
									case 6:
										ComandoAEnviarANivelInferior = toPrint.substring(11);
										break;
									default:
										ComandoAEnviarANivelInferior = "39";
										break;
								}
							}else{
								Log.i(" (RxSup) Comando Recibido",toPrint.substring(0,40));
							}
						}
				
					}catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}	
			}
		}
	}
	
/* ================================================================================================== */
	public byte[] TablaComandos(){
		Log.i(" (TxInf) Comando A Enviar Nivel Inferior", ""+ComandoAEnviarANivelInferior);
		if(ComandoAEnviarANivelInferior.equals("39")){
			byte [] ValorCampo = ComandosExploracion();
			return ValorCampo;
		}else if (ComandoAEnviarANivelInferior.regionMatches(8,"5", 0, 1)){	
			byte[] ValorCampo = new byte[6];
			ValorCampo[0]= (byte) (int) Integer.decode(ComandoAEnviarANivelInferior.substring(0, 4)); 
			ValorCampo[1]= (byte) (int) Integer.decode(ComandoAEnviarANivelInferior.substring(5, 9));
			ValorCampo[2]= (byte) (int) Integer.decode(ComandoAEnviarANivelInferior.substring(10, 14));
			ValorCampo[3]= (byte) (int) Integer.decode(ComandoAEnviarANivelInferior.substring(15,19));
			ValorCampo[4]= (byte) (int) Integer.decode(ComandoAEnviarANivelInferior.substring(20,24));
			ValorCampo[5]= (byte) (int) Integer.decode(ComandoAEnviarANivelInferior.substring(25));
			return ValorCampo;
		}else if (ComandoAEnviarANivelInferior.regionMatches(8,"6", 0, 1)){	
			byte[] ValorCampo = new byte[6];
			ValorCampo[0]= (byte) (int) Integer.decode(ComandoAEnviarANivelInferior.substring(0, 4)); 
			ValorCampo[1]= (byte) (int) Integer.decode(ComandoAEnviarANivelInferior.substring(5, 9));
			ValorCampo[2]= (byte) (int) Integer.decode(ComandoAEnviarANivelInferior.substring(10, 14));
			ValorCampo[3]= (byte) (int) Integer.decode(ComandoAEnviarANivelInferior.substring(15,19));
			ValorCampo[4]= (byte) (int) Integer.decode(ComandoAEnviarANivelInferior.substring(20,24));
			ValorCampo[5]= (byte) (int) Integer.decode(ComandoAEnviarANivelInferior.substring(25));
			return ValorCampo;
		}
		byte[] ValorCampo = {1};  //dummy
		return ValorCampo;
	}
	
/* ================================================================================================== */
	public int RecuperaID(Uri tabla){
		int ValorCampo =0;
    	Uri allTitles = tabla;
		Cursor c = getContentResolver().query(allTitles, null, null, null,null);
		if(c.getCount() == 0){
			ValorCampo = 0;
		}else{
		c.moveToFirst();
		ValorCampo=c.getInt(c.getColumnIndex("_id"));
		c.close();/** EL ERROR QUE MARCABA ANTES FUE A CAUSA DE NO CERRAR EL OBJETO CURSOR*/
		}
		return ValorCampo;
	}
	
/* ================================================================================================== */
	private int ConsultaContentProvider(String campo) {
		int ValorCampo=0;
		Uri allTitles = Uri
				.parse("content://com.nebula.labinal/puerto/1");
		Cursor c = getContentResolver()
				.query(allTitles, null, null, null, null);
		if(c.getCount()==0){
			
		}else{
		c.moveToFirst();
		ValorCampo = c.getInt(c.getColumnIndex(campo));
		c.close();
		/** EL ERROR QUE MARCABA ANTES FUE A CAUSA DE NO CERRAR EL OBJETO CURSOR */
		}
		return ValorCampo;
		
	}
	
/* ================================================================================================== */
	private String ConsultaContentProviderExploracion(String campo,int cmd ) {
		String ValorCampo=null;
		Uri allTitles = Uri
				.parse("content://com.nebula.labinal/cmdexplora/"+""+cmd);
		Cursor c = getContentResolver()
				.query(allTitles, null, null, null, null);
		if(c.getCount()==0){
			
		}else{
		c.moveToFirst();
		ValorCampo = c.getString(c.getColumnIndex(campo));
		c.close();
		/** EL ERROR QUE MARCABA ANTES FUE A CAUSA DE NO CERRAR EL OBJETO CURSOR */
		}
		return ValorCampo;
		
	}

/* ================================================================================================== */
	private byte[] ComandosExploracion(){
		byte[] comando= new byte[6];
		if(CmdExpToca==1){
		String temporal = ConsultaContentProviderExploracion("cmdexp",1);
		
		Log.i(" (TxInf) Comando de Exploracion", temporal.substring(0, 4)+" "
				           +temporal.substring(5, 9)+" "
				           +temporal.substring(10, 14)+" "
				           +temporal.substring(15,19)+" "
				           +temporal.substring(20,24)+" "
				           +temporal.substring(25,29));
		//Permite decodificar los string que vienen con formato de numero ej "0x80"
		comando[0]= (byte) (int) Integer.decode(temporal.substring(0, 4)); 
		comando[1]= (byte) (int) Integer.decode(temporal.substring(5, 9));
		comando[2]= (byte) (int) Integer.decode(temporal.substring(10, 14));
		comando[3]= (byte) (int) Integer.decode(temporal.substring(15,19));
		comando[4]= (byte) (int) Integer.decode(temporal.substring(20,24));
		comando[5]= (byte) (int) Integer.decode(temporal.substring(25,29));

		}else if(CmdExpToca==2){
			
			String temporal = ConsultaContentProviderExploracion("cmdexp",2);
			
			Log.i(" (TxInf) Comando de Exploracion", temporal.substring(0, 4)+" "
					           +temporal.substring(5, 9)+" "
					           +temporal.substring(10, 14)+" "
					           +temporal.substring(15,19)+" "
					           +temporal.substring(20,24)+" "
					           +temporal.substring(25,29));
			//Permite decodificar los string que vienen con formato de numero ej "0x80"
			comando[0]= (byte) (int) Integer.decode(temporal.substring(0, 4)); 
			comando[1]= (byte) (int) Integer.decode(temporal.substring(5, 9));
			comando[2]= (byte) (int) Integer.decode(temporal.substring(10, 14));
			comando[3]= (byte) (int) Integer.decode(temporal.substring(15,19));
			comando[4]= (byte) (int) Integer.decode(temporal.substring(20,24));
			comando[5]= (byte) (int) Integer.decode(temporal.substring(25,29));
//            if (contadorcmando == 10){
//                CmdExpToca =1;
//            }
//            contadorcmando++;
		}
		return comando;
	}

/* ================================================================================================== */
	@SuppressWarnings("unused")
	private String Acomoda39Medidor(String Respuesta){
		String ValorCampo = null;
		String temp = Respuesta;
		Uri Campos =Uri.parse("content://com.nebula.labinal/confg39/1");
		Cursor c = getContentResolver().query(Campos,null,null,null,null);
		c.moveToFirst();
		
		
		// verificamos que la respuesta este completa
		try {
			String encabezado39 = String.valueOf(c.getString(c.getColumnIndex("idmensaje39")));
			String Direccion = String.valueOf(c.getString(c.getColumnIndex("direccion")));
			c.close();
			ValorCampo= encabezado39+"26000"+ Direccion+"39"+temp.substring(6, 18)
					 .concat(temp.substring(22, 34))
					 .concat(temp.substring(38,42))
					 .concat(temp.substring(66,78))
					 .concat(temp.substring(90,114));
		} catch (StringIndexOutOfBoundsException e) {
			ValorCampo = Acomoda99();
		}
		
		
		return ValorCampo ;
	}

/* ================================================================================================== */
	private String Acomoda99(){
		String ValorCampo = null;
		Uri Campos =Uri.parse("content://com.nebula.labinal/confg39/1");
		Cursor c = getContentResolver().query(Campos,null,null,null,null);
		c.moveToFirst();
		
		String encabezado39 = String.valueOf(c.getString(c.getColumnIndex("idmensaje39")));
		String Direccion = String.valueOf(c.getString(c.getColumnIndex("direccion")));
		c.close();
		
		ValorCampo= encabezado39+"26000"+ Direccion+"99";
		return ValorCampo ;
	}

/* ================================================================================================== */
	private String Acomoda39Termos(String Respuesta){
		String ValorCampo = null;
		String temp = Respuesta;
		Uri Campos =Uri.parse("content://com.nebula.labinal/confg39/1");
		
		Cursor c = getContentResolver().query(Campos,null,null,null,null);
		c.moveToFirst();
		
		// verificamos que la respuesta este completa
		try {
			String encabezado39 = String.valueOf(c.getString(c.getColumnIndex("idmensaje39")));
			String Direccion = String.valueOf(c.getString(c.getColumnIndex("direccion")));
			c.close();
			ValorCampo= encabezado39+"26000"+ Direccion+"39"+temp.substring(6,70);
			
			
		} catch (StringIndexOutOfBoundsException e) {
			ValorCampo = Acomoda99();
		}
		
		return ValorCampo ;
	}

/* ================================================================================================== */
	private String Acomoda35(String Respuesta){
		String ValorCampo = null;
		String temp = Respuesta;
		Uri Campos =Uri.parse("content://com.nebula.labinal/confg39/1");
		
		Cursor c = getContentResolver().query(Campos,null,null,null,null);
		c.moveToFirst();
		
		String encabezado35 = String.valueOf(c.getString(c.getColumnIndex("idmensaje39")));
		String Direccion = String.valueOf(c.getString(c.getColumnIndex("direccion")));
		c.close();
		
		ValorCampo= encabezado35+"0E000"+ Direccion+"35"+temp;
		return ValorCampo ;
	}

/* ================================================================================================== */
	private String Acomoda95(){
		String ValorCampo = null;
		Uri Campos =Uri.parse("content://com.nebula.labinal/confg39/1");
		
		Cursor c = getContentResolver().query(Campos,null,null,null,null);
		c.moveToFirst();
		
		String encabezado35 = String.valueOf(c.getString(c.getColumnIndex("idmensaje39")));
		String Direccion = String.valueOf(c.getString(c.getColumnIndex("direccion")));
		c.close();
		
		ValorCampo= encabezado35+"0E000"+ Direccion+"95";
		return ValorCampo ;
	}

/* ================================================================================================== */
	@SuppressWarnings("unused")
	private int ConsultaContentProviderSegundosEnvio39() {
		int ValorCampo = 0;
		Uri allTitles = Uri
				.parse("content://com.nebula.labinal/timer/1");
		Cursor c = getContentResolver()
				.query(allTitles, null, null, null, null);
		if(c.getCount() == 0){
			ValorCampo = 0;
			return ValorCampo;
		}else{
		c.moveToFirst();
		ValorCampo = c.getInt(c.getColumnIndex("veltimer"));
		c.close();
		/** EL ERROR QUE MARCABA ANTES FUE A CAUSA DE NO CERRAR EL OBJETO CURSOR */
		return ValorCampo;
		}
	}
	
/* ================================================================================================== */
	@SuppressWarnings("unused")
	private String ConsultaContentProviderIP() {
			String ValorCampo = null;
			Uri allTitles = Uri
					.parse("content://com.nebula.labinal/ip/1");
			Cursor c = getContentResolver()
					.query(allTitles, null, null, null, null);
			if(c.getCount() == 0){
				ValorCampo= "No hay nada en la tabla";
				return ValorCampo;
			}else{
			c.moveToFirst();
			ValorCampo = c.getString(c.getColumnIndex("ip")).toString();
			c.close();
			/** EL ERROR QUE MARCABA ANTES FUE A CAUSA DE NO CERRAR EL OBJETO CURSOR */
			return ValorCampo;
			}
		}
/*========================================================================================================================*/
	
	public boolean EstadoBitED(int Estado,int noBit){
		boolean  valorBit = false;
		int Operando =0;
		switch (noBit) {
		case 1:
			Operando = 128;
			valorBit = (Estado & Operando) != 0 ;
			return valorBit;
		case 2:
			Operando = 64;
			valorBit = (Estado & Operando) != 0 ;
			return valorBit;
			

		case 3:
			Operando = 32;
			valorBit = (Estado & Operando) != 0 ;
			return valorBit;

		case 4:
			Operando = 16;
			valorBit = (Estado & Operando) != 0 ;
			return valorBit;

		case 5:
			Operando = 8;
			valorBit = (Estado & Operando) != 0 ;
			return valorBit;

		case 6:
			Operando = 4;
			valorBit = (Estado & Operando) != 0 ;
			return valorBit;
		case 7:
			Operando = 2;
			valorBit = (Estado & Operando) != 0 ;
			return valorBit;
		case 8:
			Operando = 1;
			valorBit = (Estado & Operando) != 0 ;
			return valorBit;

		default:
			valorBit = false;
			return valorBit;
		}	
	}
/*========================================================================================================================*/
	
	public boolean EstadoBitSD(int Estado,int noBit){
		boolean  valorBit = false;
		int Operando =0;
		switch (noBit) {
		case 1:
			Operando = 128;
			valorBit = (Estado & Operando) != 0 ;
			return valorBit;
		case 2:
			Operando = 64;
			valorBit = (Estado & Operando) != 0 ;
			return valorBit;
			

		case 3:
			Operando = 32;
			valorBit = (Estado & Operando) != 0 ;
			return valorBit;

		case 4:
			Operando = 16;
			valorBit = (Estado & Operando) != 0 ;
			return valorBit;

		case 5:
			Operando = 8;
			valorBit = (Estado & Operando) != 0 ;
			return valorBit;

		case 6:
			Operando = 4;
			valorBit = (Estado & Operando) != 0 ;
			return valorBit;
		case 7:
			Operando = 2;
			valorBit = (Estado & Operando) != 0 ;
			return valorBit;
		case 8:
			Operando = 1;
			valorBit = (Estado & Operando) != 0 ;
			return valorBit;

		default:
			valorBit = false;
			return valorBit;
		}
	}
	/*========================================================================================================================*/	

	private double ConsultaContentProviderm() {
		double ValorCampo = 0;
		Uri allTitles = Uri
				.parse("content://com.nebula.labinal/cmd/1");
		Cursor c = getContentResolver()
				.query(allTitles, null, null, null, null);
		if(c.getCount() == 0){
			ValorCampo = 0;
			return ValorCampo;
		}else{
		c.moveToFirst();
		ValorCampo = c.getDouble(c.getColumnIndex("comando"));
		c.close();
		/** EL ERROR QUE MARCABA ANTES FUE A CAUSA DE NO CERRAR EL OBJETO CURSOR */
		Log.i("M", ""+ValorCampo);
		return ValorCampo;
		}
	}
	/*========================================================================================================================*/
	private double ConsultaContentProviderb() {
		double ValorCampo = 0;
		Uri allTitles = Uri
				.parse("content://com.nebula.labinal/cmd/2");
		Cursor c = getContentResolver()
				.query(allTitles, null, null, null, null);
		if(c.getCount() == 0){
			ValorCampo = 0;
			return ValorCampo;
		}else{
		c.moveToFirst();
		ValorCampo = c.getDouble(c.getColumnIndex("comando"));
		c.close();
		/** EL ERROR QUE MARCABA ANTES FUE A CAUSA DE NO CERRAR EL OBJETO CURSOR */
		Log.i("B", ""+ValorCampo);
		return ValorCampo;
		}
	}
	Uri uri;
	int Rx = 1;
	// Para madar cada tiempo determnado
	int ContadorEnvia39 = 1;
	int cont =1;
	String ComandoNivelSuperior = "";
	String ComandoAEnviarANivelInferior = "39";
	HttpURLConnection urlConnection = null;
	int ContadorEnviaExploracion =1;
	int Contador9995 =1;
	String temp;
	
	//datos display
	ImageView Pcerradauno;
	ImageView Pcerradados;
	ImageView conf ;
	ImageView venton ;
	ImageView Pabiertados;
	ImageView Pabiertauno;
	ImageView reson ;
    ImageView focoreson1;
    ImageView reson2 ;
    ImageView reson3 ;
	ImageView flechauno;
	ImageView flechados;
	TextView proceso;
	TextView temperatura;
	ToggleButton tg;
	ImageView verde ;
	ImageView rojo ;
	AbsoluteLayout Pantallaconf;
	AbsoluteLayout PantallaPrincipal;
	int CmdExpToca = 2;
	TextView max;
	TextView min;
	TextView TC;
	ImageView GuardaMax;
	ImageView GuardaMin;
	ImageView GuardarTC;
	EditText NvoTC;
	EditText NvoMax;
	EditText NvoMin;
	int TempMax=0;
	int TempMin=0;
	double TiempototalDeCuraro=0;
	TextView totalminTC;
	TextView TCtranscurrido;
	ImageView paroemergencia;
    int contadorcmando =0 ;

}