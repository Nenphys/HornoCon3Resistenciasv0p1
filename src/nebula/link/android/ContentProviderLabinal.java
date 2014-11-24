package nebula.link.android;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class ContentProviderLabinal extends ContentProvider {

	private static class DatabaseHelper extends SQLiteOpenHelper{
		
		DatabaseHelper(Context context) {
			super(context, NOMBRE_BASEDATOS, null, VERSION_BASEDATOS);
		}	
		@Override
		public void onCreate(SQLiteDatabase db){
			//tabla mensajes
			db.execSQL(CREAR_BASEDATOS);
			//tabla comando
			db.execSQL(CREA_TABLA_COMANDO);
			//tabla cnf 39
			db.execSQL(CREA_TABLA_CONF39);
			//tabla confg puerto
			db.execSQL(CREA_TABLA_CONFPUERTO);
			//tabla de comandos de exploracion
			db.execSQL(CREA_TABLA_CMDEXPLORA);
			//tabla que tiene la ip del servidor
			db.execSQL(CREA_TABLA_IPSERVIDOR);
			//tabla que tiene la Velocidad del timer task
			db.execSQL(CREA_TABLA_VELTIMER);
		}
		
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion,
				int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS mensajes ");
			onCreate(db);
			
		}
	}
	

	@Override
	public int delete(Uri arg0, String arg1, String[] arg2) {
		int count=0;
		
		
		//tabla Mensajes
		switch (uriMatcher.match(arg0)){
		case MODBUS:
		count = PruebaBD.delete(
		TABLA_BASEDATOS,
		arg1,
		arg2);
		break;
		case MODBUS_ID:
		String id = arg0.getPathSegments().get(1);
		count = PruebaBD.delete(
		TABLA_BASEDATOS,
		_ID + " = " + id +
		(!TextUtils.isEmpty(arg1) ? " AND (" +
		arg1 + ')' : ""),
		arg2);
		break;
		
		//tabla Comandos
		case CMD:
			count = PruebaBD.delete(
			TABLA_COMANDOS,
			arg1,
			arg2);
			break;
			case CMD_ID:
			String idCMD = arg0.getPathSegments().get(1);
			count = PruebaBD.delete(
			TABLA_COMANDOS,
			_ID + " = " + idCMD +
			(!TextUtils.isEmpty(arg1) ? " AND (" +
			arg1 + ')' : ""),
			arg2);
			break;
		default: throw new IllegalArgumentException("Unknown URI " + arg0);
		}
		
		getContext().getContentResolver().notifyChange(arg0, null);
		return count;
	}

	@Override
	public String getType(Uri uri) {
		switch (uriMatcher.match(uri)){
				//mensajes toda la tabla
			case MODBUS:
				return "vnd.android.cursor.dir/com.nebula.labinal";
				//Mensajes 1 item
			case MODBUS_ID:
				return "vnd.android.cursor.item/com.nebula.labinal";
				
				
				//Comando toda la tabla
			case CMD:
				return "vnd.android.cursor.dir/com.nebula.labinal";	
				//Comando 1 item
			case CMD_ID:
				return "vnd.android.cursor.item/com.nebula.labinal";
			case CONFG39:
				return "vnd.android.cursor.dir/com.nebula.labinal";	
				//Comando 1 item
			case CONFG39_ID:
				return "vnd.android.cursor.item/com.nebula.labinal";
			case PUERTO:
				return "vnd.android.cursor.dir/com.nebula.labinal";	
				//Comando 1 item
			case PUERTO_ID:
				return "vnd.android.cursor.item/com.nebula.labinal";
			case CMDEXPLORA:
				return "vnd.android.cursor.dir/com.nebula.labinal";	
				//Comando 1 item
			case CMDEXPLORA_ID:
				return "vnd.android.cursor.item/com.nebula.labinal";
			default:
				throw new IllegalArgumentException("URI no admitida: " + uri);
			}

	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		long rowID = PruebaBD.insert(TABLA_BASEDATOS,"",values);	
		long rowID1 = PruebaBD.insert(TABLA_COMANDOS,"",values);
		Uri _uri=null;
		switch (uriMatcher.match(uri)){
		//---obtener todos los mensajes
		case MODBUS:
				if (rowID>0)
				{
					_uri = ContentUris.withAppendedId(CONTENT_URI, rowID);
					getContext().getContentResolver().notifyChange(_uri, null);
			
				}
			break;
		
		case CMD:
			  if (rowID1>0)
				{
					_uri = ContentUris.withAppendedId(CONTENT_URICMD, rowID1);
					getContext().getContentResolver().notifyChange(_uri, null);
				
				}
			  break;
			default:
			throw new IllegalArgumentException("URI no admitida: " + uri);
		}
			
			return _uri;
			
		
	}

	@Override
	public boolean onCreate() {
		Context context = getContext();
		DatabaseHelper dbHelper = new DatabaseHelper(context);
		PruebaBD = dbHelper.getWritableDatabase();
		PruebaBD = dbHelper.getReadableDatabase();
		return (PruebaBD == null)? false:true; //es un If
		
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		
		SQLiteQueryBuilder sqlBuilder = new SQLiteQueryBuilder();
		// Query para las tablas
		switch (uriMatcher.match(uri)){
		case MODBUS:
			sqlBuilder.setTables(TABLA_BASEDATOS);
			if (sortOrder==null || sortOrder=="")
				sortOrder = _ID;
			break;
		case MODBUS_ID:
			sqlBuilder.setTables(TABLA_BASEDATOS);
			Log.i("", "tablas "+sqlBuilder.getTables());
			Log.i("","Matcher "+uri);
			sqlBuilder.appendWhere(_ID + " = " + uri.getPathSegments().get(1));
			if (sortOrder==null || sortOrder=="")
				sortOrder = _ID;
		break;
		case CMD:
			sqlBuilder.setTables(TABLA_COMANDOS);
			if (sortOrder==null || sortOrder=="")
				sortOrder = _ID;
			break;
		case CMD_ID:
			sqlBuilder.setTables(TABLA_COMANDOS);
			Log.i("", "tablas "+sqlBuilder.getTables());
			Log.i("","Matcher "+uri);
			sqlBuilder.appendWhere(_ID + " = " + uri.getPathSegments().get(1));
			if (sortOrder==null || sortOrder=="")
				sortOrder = _ID;
		break;
		
		case CONFG39:
			sqlBuilder.setTables(TABLA_CONFG39);
			if (sortOrder==null || sortOrder=="")
				sortOrder = _ID;
			break;
		case CONFG39_ID:
			sqlBuilder.setTables(TABLA_CONFG39);
			Log.i("", "tablas "+sqlBuilder.getTables());
			Log.i("","Matcher "+uri);
			sqlBuilder.appendWhere(_ID + " = " + uri.getPathSegments().get(1));
			if (sortOrder==null || sortOrder=="")
				sortOrder = _ID;
		break;
		
		case PUERTO:
			sqlBuilder.setTables(TABLA_CONFGPUERTO);
			if (sortOrder==null || sortOrder=="")
				sortOrder = _ID;
			break;
		case PUERTO_ID:
			sqlBuilder.setTables(TABLA_CONFGPUERTO);
			Log.i("", "tablas "+sqlBuilder.getTables());
			Log.i("","Matcher "+uri);
			sqlBuilder.appendWhere(_ID + " = " + uri.getPathSegments().get(1));
			if (sortOrder==null || sortOrder=="")
				sortOrder = _ID;
		break;
		case CMDEXPLORA:
			sqlBuilder.setTables(TABLA_CMDEXPLORA);
			if (sortOrder==null || sortOrder=="")
				sortOrder = _ID;
			break;
		case CMDEXPLORA_ID:
			sqlBuilder.setTables(TABLA_CMDEXPLORA);
			Log.i("", "tablas "+sqlBuilder.getTables());
			Log.i("","Matcher "+uri);
			sqlBuilder.appendWhere(_ID + " = " + uri.getPathSegments().get(1));
			if (sortOrder==null || sortOrder=="")
				sortOrder = _ID;
			break;
		case IP:
			sqlBuilder.setTables(TABLA_IP);
			if (sortOrder==null || sortOrder=="")
				sortOrder = _ID;
			break;
		case IP_ID:
			sqlBuilder.setTables(TABLA_IP);
			Log.i("", "tablas "+sqlBuilder.getTables());
			Log.i("","Matcher "+uri);
			sqlBuilder.appendWhere(_ID + " = " + uri.getPathSegments().get(1));
			if (sortOrder==null || sortOrder=="")
				sortOrder = _ID;
		break;
		case TIMER:
			sqlBuilder.setTables(TABLA_TIMER);
			if (sortOrder==null || sortOrder=="")
				sortOrder = _ID;
			break;
		case TIMER_ID:
			sqlBuilder.setTables(TABLA_TIMER);
			Log.i("", "tablas "+sqlBuilder.getTables());
			Log.i("","Matcher "+uri);
			sqlBuilder.appendWhere(_ID + " = " + uri.getPathSegments().get(1));
			if (sortOrder==null || sortOrder=="")
				sortOrder = _ID;
		break;
		default: throw new NullPointerException("mal query"+uri);
		}
		
		
			
		Cursor c = sqlBuilder.query(
				PruebaBD,
				projection,
				selection,
				selectionArgs,
				null,
				null,
				sortOrder);
		c.setNotificationUri(getContext().getContentResolver(), uri);
		
		return c;
		
		
	}
	

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		int count = 0;
		switch (uriMatcher.match(uri)){
		case MODBUS:
			count = PruebaBD.update(TABLA_BASEDATOS,values,selection,selectionArgs);
		break;
			case MODBUS_ID:
			count = PruebaBD.update(TABLA_BASEDATOS,values,_ID + " = " + uri.getPathSegments().get(1) +
					(!TextUtils.isEmpty(selection) ? " AND (" +
			selection + ')' : ""),
			selectionArgs);
		break;
		//tabla Comandos
		case CMD:
			count = PruebaBD.update(TABLA_COMANDOS,values,selection,selectionArgs);
			break;
		case CMD_ID:
			count = PruebaBD.update(TABLA_COMANDOS,values,_ID + " = " + uri.getPathSegments().get(1) +
			(!TextUtils.isEmpty(selection) ? " AND (" +
			selection + ')' : ""),
			selectionArgs);
			break;
		case CONFG39:
			count = PruebaBD.update(TABLA_CONFG39,values,selection,selectionArgs);
			break;
		case CONFG39_ID:
			count = PruebaBD.update(TABLA_CONFG39,values,_ID + " = " + uri.getPathSegments().get(1) +
			(!TextUtils.isEmpty(selection) ? " AND (" +
			selection + ')' : ""),
			selectionArgs);
			break;
		case PUERTO:
			count = PruebaBD.update(TABLA_CONFGPUERTO,values,selection,selectionArgs);
			break;
		case PUERTO_ID:
			count = PruebaBD.update(TABLA_CONFGPUERTO,values,_ID + " = " + uri.getPathSegments().get(1) +
			(!TextUtils.isEmpty(selection) ? " AND (" +
			selection + ')' : ""),
			selectionArgs);
			break;
		case CMDEXPLORA:
			count = PruebaBD.update(TABLA_CMDEXPLORA,values,selection,selectionArgs);
			break;
		case CMDEXPLORA_ID:
			count = PruebaBD.update(TABLA_CMDEXPLORA,values,_ID + " = " + uri.getPathSegments().get(1) +
			(!TextUtils.isEmpty(selection) ? " AND (" +
			selection + ')' : ""),
			selectionArgs);
			break;
		case IP:
			count = PruebaBD.update(TABLA_IP,values,selection,selectionArgs);
			break;
		case IP_ID:
			count = PruebaBD.update(TABLA_IP,values,_ID + " = " + uri.getPathSegments().get(1) +
			(!TextUtils.isEmpty(selection) ? " AND (" +
			selection + ')' : ""),
			selectionArgs);
			break;
		case TIMER:
			count = PruebaBD.update(TABLA_TIMER,values,selection,selectionArgs);
			break;
		case TIMER_ID:
			count = PruebaBD.update(TABLA_TIMER,values,_ID + " = " + uri.getPathSegments().get(1) +
			(!TextUtils.isEmpty(selection) ? " AND (" +
			selection + ')' : ""),
			selectionArgs);
			break;
		
		default: throw new IllegalArgumentException("Unknown URI " + uri);
		}
			
		getContext().getContentResolver().notifyChange(uri, null);
		
		return count;
	}
	
	
	
	
	
	
	
		// Con esto URIS entramos a cada tabla que nesecitamos
		public static final String PROVIDER_NAME 			="com.nebula.labinal";
		public static final Uri CONTENT_URI 				=Uri.parse("content://"+ PROVIDER_NAME + "/modbus");
		public static final Uri CONTENT_URICMD 				=Uri.parse("content://"+ PROVIDER_NAME + "/cmd");
		public static final Uri CONTENT_CONFG39 			=Uri.parse("content://"+ PROVIDER_NAME + "/confg39");
		public static final Uri CONTENT_PUERTO 				=Uri.parse("content://"+ PROVIDER_NAME + "/puerto");
		public static final Uri CONTENT_CMDEXPLORA			=Uri.parse("content://"+ PROVIDER_NAME + "/cmdexplora");
		public static final Uri CONTENT_IP					=Uri.parse("content://"+ PROVIDER_NAME + "/ip");
		public static final Uri CONTENT_TIMER					=Uri.parse("content://"+ PROVIDER_NAME + "/timer");
		
		// nombre de los campos de la tabla
			//Tabla de comandos
		
		public static final String COMANDO 						= "comando";
			
		
			
			//Tabla de Modbus
		public static final String _ID 					= "_id";
		public static final String DATOSCRUDOS 			= "datoscrudos";
		public static final String ECOCMD 				= "ecocmd";
		public static final String IDMENSAJE39			= "idmensaje39";
		public static final String DIRECCION 			= "direccion";
		public static final String PROTOCOLO 			= "protocolo";
		public static final String VELOCIDAD 			= "velocidad";
		public static final String CAMBIO 				= "cambio";
		public static final String IDINTERNA			= "idinterna";
		public static final String CMDEXP 				= "cmdexp";
		
		
		// para poder sacar toda la consulta o solamente una especifica
		private static final int MODBUS 				= 1;
		private static final int MODBUS_ID				= 2;
		private static final int CMD 					= 3;
		private static final int CMD_ID					= 4;
		private static final int CONFG39 				= 5;
		private static final int CONFG39_ID				= 6;
		private static final int PUERTO 				= 7;
		private static final int PUERTO_ID				= 8;
		private static final int CMDEXPLORA 			= 9;
		private static final int CMDEXPLORA_ID			= 10;
		private static final int IP 					= 11;
		private static final int IP_ID					= 12;
		private static final int TIMER 					= 13;
		private static final int TIMER_ID				= 14;
		
	
		
		
		
		// este tiene que hacer el match con la tabla mensajes
		private static final UriMatcher uriMatcher;
		static{
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(PROVIDER_NAME, "modbus", MODBUS);
		uriMatcher.addURI(PROVIDER_NAME, "modbus/#", MODBUS_ID);
		uriMatcher.addURI(PROVIDER_NAME, "cmd", CMD);
		uriMatcher.addURI(PROVIDER_NAME, "cmd/#", CMD_ID);
		uriMatcher.addURI(PROVIDER_NAME, "confg39", CONFG39);
		uriMatcher.addURI(PROVIDER_NAME, "confg39/#", CONFG39_ID);
		uriMatcher.addURI(PROVIDER_NAME, "puerto", PUERTO);
		uriMatcher.addURI(PROVIDER_NAME, "puerto/#", PUERTO_ID);
		uriMatcher.addURI(PROVIDER_NAME, "cmdexplora", CMDEXPLORA);
		uriMatcher.addURI(PROVIDER_NAME, "cmdexplora/#", CMDEXPLORA_ID);
		uriMatcher.addURI(PROVIDER_NAME, "ip", IP);
		uriMatcher.addURI(PROVIDER_NAME, "ip/#", IP_ID);
		uriMatcher.addURI(PROVIDER_NAME, "timer", TIMER);
		uriMatcher.addURI(PROVIDER_NAME, "timer/#", TIMER_ID);
		}
		
	
		//---para uso base datos---
		private SQLiteDatabase PruebaBD;
		private static final String NOMBRE_BASEDATOS 		= "termos";
		private static final String TABLA_BASEDATOS 		= "mensajes";
		private static final String TABLA_COMANDOS			= "comandos";
		private static final String TABLA_CONFG39			= "confg39";
		private static final String TABLA_CONFGPUERTO		= "confgpuerto";
		private static final String TABLA_CMDEXPLORA		= "cmdexplora";
		private static final String TABLA_IP				= "ip";
		private static final String TABLA_TIMER				= "timer";
		private static final int VERSION_BASEDATOS = 1;
		
		
		
		
		// Se Cren las tablas que necesita la base de datos
		private static final String CREAR_BASEDATOS =
				"create table " + TABLA_BASEDATOS+
				" (_id integer primary key autoincrement, datoscrudos text not null);";
		
		private static final String CREA_TABLA_COMANDO =
			"create table "+ TABLA_COMANDOS+ " (_id integer primary key autoincrement, comando integer not null);";
		
		private static final String CREA_TABLA_CONF39 =
			"create table "+ TABLA_CONFG39+ " (_id integer primary key autoincrement, idmensaje39 integer not null, " +
					"direccion integer not null);";
		
		private static final String CREA_TABLA_CONFPUERTO =
			"create table "+ TABLA_CONFGPUERTO+ " (_id integer primary key autoincrement, protocolo text not null," +
					"velocidad integer not null, cambio integer not null);";
		
		private static final String CREA_TABLA_CMDEXPLORA =
			"create table "+ TABLA_CMDEXPLORA+ " (_id integer primary key autoincrement, idinterna integer not null," +
					"cmdexp text not null);";
		
		private static final String CREA_TABLA_IPSERVIDOR =
			"create table "+ TABLA_IP+ " (_id integer primary key autoincrement, ip text not null);";
		
		private static final String CREA_TABLA_VELTIMER =
			"create table "+ TABLA_TIMER+ " (_id integer primary key autoincrement, veltimer integer not null);";
		
}