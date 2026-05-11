package com.alfanar.villaroom.util;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.alfanar.villaroom.App;
import com.alfanar.villaroom.R;
import com.alfanar.villaroom.models.CallModel;
import com.alfanar.villaroom.models.Camera2Model;
import com.alfanar.villaroom.models.CameraDevice;
import com.alfanar.villaroom.models.ImagesModel;
import com.alfanar.villaroom.models.MenuModel;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;

public class DatabaseHelper extends SQLiteOpenHelper {

    static final String DB_NAME = "VILLA_ROOM.DB";
    static final int DB_VERSION = 10;

    private static volatile DatabaseHelper instance;

    private final String TABLE_CAMERAS = "CAMERAS";
    private final String TABLE_CAMERAS2 = "CAMERAS2";
    private final String TABLE_CALLS = "CALLS";
    private final String TABLE_IMAGES = "IMAGES";
    private final String TABLE_MENU = "MENU";

    private final String MENU_ITEM_ID = "menuItemID";
    private final String MENU_ITEM_TAG = "menuItemTag";
    private final String MENU_ITEM_REAL_POS = "menuItemRealPos";
    private final String MENU_ITEM_VISIBILITY = "menuItemVisibility";

    private final String CAMERA_ID = "cameraID";
    private final String CAMERA_NAME = "cameraName";
    private final String CAMERA_ADDRESS = "cameraAddress";
    private final String CAMERA_USER = "cameraUserName";
    private final String CAMERA_PASS = "cameraPass";
    private final String CAMERA_IP = "cameraIp";

    private final String IMAGE_ID = "imageID";
    private final String IMAGE_PATH = "imagePath";
    private final String IMAGE_TIME = "imageTime";
    private final String IMAGE_MAC_ADDRESS = "imageMacAddress";

    private final String CALL_ID = "callID";
    private final String CALL_FROM = "callFrom";
    private final String CALL_TO = "callTo";
    private final String CALL_TYPE = "callType";
    private final String CALL_DATA = "callData";
    private final String CALL_STATE = "callState";
    private final String CALL_DATE = "callDate";
    private final String CALL_IMG_PATH = "callImgPath";
    private final String CALLER_NAME = "callerName";
    private final String CALL_READ_STATE = "callReadState";

    private DatabaseHelper() {
        super(App.getInstance().getApplicationContext(), DB_NAME, null, DB_VERSION);
    }

    public static DatabaseHelper getInstance() {
        if (instance == null) {
            synchronized (DatabaseHelper.class) {
                if (instance == null) {
                    instance = new DatabaseHelper();
                }
            }
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        ensureSchema(db);
        ensureIndexes(db);
        Logger.d("DatabaseHelper.onCreate");
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        ensureSchema(db);
        ensureIndexes(db);
    }

    /**
     * DB_VERSION artırmadan bile tablo/index yoksa oluşturulur.
     * Kolon isimleri aynı kalır.
     */
    private void ensureSchema(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_MENU + "("
                + MENU_ITEM_ID + " TEXT,"
                + MENU_ITEM_TAG + " TEXT,"
                + MENU_ITEM_REAL_POS + " INTEGER,"
                + MENU_ITEM_VISIBILITY + " INTEGER"
                + ")");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_CAMERAS + "("
                + CAMERA_ID + " INTEGER PRIMARY KEY,"
                + CAMERA_NAME + " TEXT,"
                + CAMERA_ADDRESS + " TEXT"
                + ")");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_CALLS + "("
                + CALL_ID + " TEXT,"
                + CALL_FROM + " TEXT,"
                + CALL_TO + " TEXT,"
                + CALL_TYPE + " TEXT,"
                + CALL_DATA + " TEXT,"
                + CALL_STATE + " TEXT,"
                + CALL_DATE + " TEXT,"
                + CALL_IMG_PATH + " TEXT,"
                + CALLER_NAME + " TEXT,"
                + CALL_READ_STATE + " INTEGER"
                + ")");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_IMAGES + "("
                + IMAGE_ID + " INTEGER,"
                + IMAGE_PATH + " TEXT,"
                + IMAGE_TIME + " TEXT,"
                + IMAGE_MAC_ADDRESS + " TEXT"
                + ")");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_CAMERAS2 + "("
                + CAMERA_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + CAMERA_NAME + " TEXT,"
                + CAMERA_IP + " TEXT,"
                + CAMERA_USER + " TEXT,"
                + CAMERA_PASS + " TEXT"
                + ")");
    }

    private void ensureIndexes(SQLiteDatabase db) {
        // Kolon isimlerine müdahale değil; performans için index.
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_calls_date ON " + TABLE_CALLS + "(" + CALL_DATE + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_calls_read ON " + TABLE_CALLS + "(" + CALL_READ_STATE + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_images_time ON " + TABLE_IMAGES + "(" + IMAGE_TIME + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_menu_tag ON " + TABLE_MENU + "(" + MENU_ITEM_TAG + ")");
    }

    /**
     * Artık DROP TABLE yok (veri kaybı olmasın).
     * Versiyon artırmadığın için zaten tetiklenmeyecek, ama ilerisi için güvenli.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        ensureSchema(db);
        ensureIndexes(db);
        Logger.d("DatabaseHelper.onUpgrade old=" + oldVersion + " new=" + newVersion);
    }

    // -------------------- Helpers --------------------

    private interface TxWork {
        void run(SQLiteDatabase db) throws Exception;
    }

    private void inTx(TxWork work) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            work.run(db);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
        } finally {
            db.endTransaction();
        }
    }

    private long queryLong(String sql, String[] args, long def) {
        try (Cursor c = getReadableDatabase().rawQuery(sql, args)) {
            if (c.moveToFirst()) return c.getLong(0);
        } catch (Exception e) {
            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
        }
        return def;
    }

    private boolean existsRow(String table, String selection, String[] args) {
        String sql = "SELECT 1 FROM " + table + " WHERE " + selection + " LIMIT 1";
        try (Cursor c = getReadableDatabase().rawQuery(sql, args)) {
            return c.moveToFirst();
        } catch (Exception e) {
            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
            return false;
        }
    }

    private static void safeDeleteFile(String path) {
        if (path == null || path.isEmpty()) return;
        try {
            File f = new File(path);
            if (f.exists()) {
                boolean res = f.delete();
                Logger.d("file removing res = " + res);
            } else {
                Logger.d("file not exist");
            }
        } catch (Exception e) {
            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
        }
    }

    // -------------------- INIT / FACTORY --------------------

    // rtsp://10.99.28.12:554/media/video1/metadata
    // rtsp://admin:123456@192.168.1.180:540/media/video1
    public synchronized void initDBValues() {
        // Schema garanti
        SQLiteDatabase db = getWritableDatabase();
        ensureSchema(db);
        ensureIndexes(db);

        menuItemsControl();

        ArrayList<CameraDevice> exCameras = getAllCameras();
        if (!exCameras.isEmpty()) {
            for (CameraDevice dev : exCameras) {
                try {
                    String addr = dev.getAddress();
                    String ip;

                    if (addr != null && addr.contains("@")) {
                        String[] arr = addr.split("@");
                        String[] arr2 = arr[1].split(":");
                        ip = arr2[0];
                    } else {
                        // "rtsp://x.x.x.x:..." gibi
                        String str = (addr != null && addr.length() >= 7) ? addr.substring(7) : "";
                        String[] arr2 = str.split(":");
                        ip = arr2[0];
                    }

                    insertCamera2(new Camera2Model(0, dev.getName(), ip, "admin", "admin"));
                    deleteCamera(dev.getId());
                } catch (Exception e) {
                    Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
                }
            }
        }
    }

    public void resetFactory() {
        deleteAllCalls();
        deleteAllImages();

        inTx(db -> db.delete(TABLE_MENU, null, null));
    }

    // -------------------- MENU --------------------

    private void menuItemsControl() {
        boolean existsSecurity = existsRow(TABLE_MENU, MENU_ITEM_TAG + "=?", new String[]{"security"});

        String[] tags = {"historyCalls", "cameras", "roomsDoors", "smartHome", "gallery"};

        if (existsSecurity) {
            // Eski "security" menüsü varsa komple resetleyip 5 item ekle
            inTx(db -> db.delete(TABLE_MENU, null, null));

            for (int i = 0; i < 5; i++) {
                MenuModel model = new MenuModel(String.valueOf(i), tags[i], i, true);
                insertMenuItem(model);
            }
            return;
        }

        // Yoksa eksik olanları tamamla
        for (int i = 0; i < 5; i++) {
            boolean exists = existsRow(TABLE_MENU, MENU_ITEM_ID + "=?", new String[]{String.valueOf(i)});
            if (!exists) {
                MenuModel model = new MenuModel(String.valueOf(i), tags[i], i, true);
                insertMenuItem(model);
            }
        }
    }

    public void insertMenuItem(MenuModel model) {
        inTx(db -> {
            ContentValues cv = new ContentValues();
            cv.put(MENU_ITEM_ID, model.getItemId());
            cv.put(MENU_ITEM_TAG, model.getItemTag());
            cv.put(MENU_ITEM_REAL_POS, model.getItemPosition());
            cv.put(MENU_ITEM_VISIBILITY, model.isItemVisibility() ? 1 : 0);
            db.insert(TABLE_MENU, null, cv);
        });
    }

    public void updateMenuItem(MenuModel model) {
        inTx(db -> {
            ContentValues cv = new ContentValues();
            cv.put(MENU_ITEM_TAG, model.getItemTag());
            cv.put(MENU_ITEM_REAL_POS, model.getItemPosition());
            cv.put(MENU_ITEM_VISIBILITY, model.isItemVisibility() ? 1 : 0);
            db.update(TABLE_MENU, cv, MENU_ITEM_ID + " = ?", new String[]{model.getItemId()});
        });
    }

    @SuppressLint("Range")
    public ArrayList<MenuModel> getAllMenuItems() {
        String[] name = {
                App.getInstance().getResources().getString(R.string.history_calls),
                App.getInstance().getResources().getString(R.string.cameras),
                App.getInstance().getResources().getString(R.string.rooms_doors),
                App.getInstance().getResources().getString(R.string.smarthome),
                App.getInstance().getResources().getString(R.string.gallery)
        };

        int[] icon = {
                R.drawable.ic_history_01,
                R.drawable.ic_kamera_01,
                R.drawable.ic_outdoor_01,
                R.drawable.ic_smart_home,
                R.drawable.ic_gallery
        };

        ArrayList<MenuModel> list = new ArrayList<>();

        try (Cursor cursor = getReadableDatabase().rawQuery("SELECT * FROM " + TABLE_MENU, null)) {
            while (cursor.moveToNext()) {
                String tag = cursor.getString(cursor.getColumnIndex(MENU_ITEM_TAG));
                if ("security".equals(tag)) continue;

                String itemId = cursor.getString(cursor.getColumnIndex(MENU_ITEM_ID));
                int pos = cursor.getInt(cursor.getColumnIndex(MENU_ITEM_REAL_POS));
                boolean visibility = cursor.getInt(cursor.getColumnIndex(MENU_ITEM_VISIBILITY)) == 1;

                MenuModel model = new MenuModel(itemId, tag, pos, visibility);

                int idx = 0;
                try { idx = Integer.parseInt(itemId); } catch (Exception ignore) {}
                if (idx >= 0 && idx < name.length) model.setItemName(name[idx]);
                if (idx >= 0 && idx < icon.length) model.setItemIconId(icon[idx]);

                list.add(model);
            }
        } catch (Exception e) {
            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
        }

        return list;
    }

    // -------------------- CALLS --------------------


    public void insertCall(CallModel callModel) {
        long count = queryLong("SELECT COUNT(*) FROM " + TABLE_CALLS, null, 0);

        inTx(db -> {
            if (count >= 10000) {
                // En eski 500 kaydı sil (deterministik)
                db.execSQL("DELETE FROM " + TABLE_CALLS +
                        " WHERE rowid IN (" +
                        "   SELECT rowid FROM " + TABLE_CALLS +
                        "   ORDER BY CAST(" + CALL_DATE + " AS INTEGER) ASC LIMIT 500" +
                        ")");
            }

            ContentValues cv = new ContentValues();
            cv.put(CALL_ID, callModel.getCallId());
            cv.put(CALL_FROM, callModel.getCallFrom());
            cv.put(CALL_TO, callModel.getCallTo());
            cv.put(CALL_TYPE, callModel.getCallType());
            cv.put(CALL_DATE, callModel.getCallDate());
            cv.put(CALL_STATE, callModel.getCallState());
            cv.put(CALL_IMG_PATH, callModel.getCallImgPath());
            cv.put(CALLER_NAME, callModel.getCallerName());
            cv.put(CALL_DATA, callModel.getCallData());
            cv.put(CALL_READ_STATE, callModel.isCallReadState() ? 1 : 0);

            db.insert(TABLE_CALLS, null, cv);
        });

        if (count >= 10000) {
            MyUtils.getInstance().controlCallImages();
        }
    }

    @SuppressLint("Range")
    public void insertCallTest(ArrayList<CallModel> list) {
        inTx(db -> {
            for (CallModel callModel : list) {
                ContentValues cv = new ContentValues();
                cv.put(CALL_ID, callModel.getCallId());
                cv.put(CALL_FROM, callModel.getCallFrom());
                cv.put(CALL_TO, callModel.getCallTo());
                cv.put(CALL_TYPE, callModel.getCallType());
                cv.put(CALL_DATE, callModel.getCallDate());
                cv.put(CALL_STATE, callModel.getCallState());
                cv.put(CALL_IMG_PATH, callModel.getCallImgPath());
                cv.put(CALLER_NAME, callModel.getCallerName());
                cv.put(CALL_DATA, callModel.getCallData());
                cv.put(CALL_READ_STATE, callModel.isCallReadState() ? 1 : 0);
                db.insert(TABLE_CALLS, null, cv);
            }
        });
    }

    public void setAllCallReadState() {
        inTx(db -> {
            ContentValues cv = new ContentValues();
            cv.put(CALL_READ_STATE, 1);
            db.update(TABLE_CALLS, cv, null, null);
        });
    }

    public void updateCall(CallModel callModel) {
        inTx(db -> {
            ContentValues cv = new ContentValues();
            cv.put(CALL_ID, callModel.getCallId());
            cv.put(CALL_FROM, callModel.getCallFrom());
            cv.put(CALL_TO, callModel.getCallTo());
            cv.put(CALL_TYPE, callModel.getCallType());
            cv.put(CALL_DATE, callModel.getCallDate());
            cv.put(CALL_STATE, callModel.getCallState());
            cv.put(CALL_IMG_PATH, callModel.getCallImgPath());
            cv.put(CALLER_NAME, callModel.getCallerName());
            cv.put(CALL_DATA, callModel.getCallData());
            cv.put(CALL_READ_STATE, callModel.isCallReadState() ? 1 : 0);

            db.update(TABLE_CALLS, cv, CALL_ID + " = ?", new String[]{callModel.getCallId()});
        });
    }

    @SuppressLint("Range")
    public ArrayList<CallModel> getAllCalls() {
        ArrayList<CallModel> list = new ArrayList<>();

        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT * FROM " + TABLE_CALLS +
                        " ORDER BY CAST(" + CALL_DATE + " AS INTEGER) ASC",
                null
        )) {
            while (cursor.moveToNext()) {
                CallModel m = new CallModel();
                m.setCallId(cursor.getString(cursor.getColumnIndex(CALL_ID)));
                m.setCallFrom(cursor.getString(cursor.getColumnIndex(CALL_FROM)));
                m.setCallTo(cursor.getString(cursor.getColumnIndex(CALL_TO)));
                m.setCallType(cursor.getString(cursor.getColumnIndex(CALL_TYPE)));
                m.setCallDate(cursor.getString(cursor.getColumnIndex(CALL_DATE)));
                m.setCallState(cursor.getString(cursor.getColumnIndex(CALL_STATE)));
                m.setCallImgPath(cursor.getString(cursor.getColumnIndex(CALL_IMG_PATH)));
                m.setCallerName(cursor.getString(cursor.getColumnIndex(CALLER_NAME)));
                m.setCallData(cursor.getString(cursor.getColumnIndex(CALL_DATA)));
                m.setCallReadState(cursor.getInt(cursor.getColumnIndex(CALL_READ_STATE)) != 0);
                list.add(m);
            }
        } catch (Exception e) {
            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
        }

        return list;
    }

    public long getAllCallsFirst() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2022, 6, 1); // orijinal mantık
        long def = calendar.getTimeInMillis();

        return queryLong("SELECT MIN(CAST(" + CALL_DATE + " AS INTEGER)) FROM " + TABLE_CALLS, null, def);
    }

    public long getAllCallsLast() {
        long def = System.currentTimeMillis();
        return queryLong("SELECT MAX(CAST(" + CALL_DATE + " AS INTEGER)) FROM " + TABLE_CALLS, null, def);
    }

    @SuppressLint("Range")
    public ArrayList<CallModel> getAllCallsWithDate(long startDate, long endDate) {
        ArrayList<CallModel> list = new ArrayList<>();

        String sql = "SELECT * FROM " + TABLE_CALLS +
                " WHERE CAST(" + CALL_DATE + " AS INTEGER) BETWEEN ? AND ?" +
                " ORDER BY CAST(" + CALL_DATE + " AS INTEGER) ASC";

        try (Cursor cursor = getReadableDatabase().rawQuery(
                sql,
                new String[]{String.valueOf(startDate), String.valueOf(endDate)}
        )) {
            while (cursor.moveToNext()) {
                CallModel m = new CallModel();
                m.setCallId(cursor.getString(cursor.getColumnIndex(CALL_ID)));
                m.setCallFrom(cursor.getString(cursor.getColumnIndex(CALL_FROM)));
                m.setCallTo(cursor.getString(cursor.getColumnIndex(CALL_TO)));
                m.setCallType(cursor.getString(cursor.getColumnIndex(CALL_TYPE)));
                m.setCallDate(cursor.getString(cursor.getColumnIndex(CALL_DATE)));
                m.setCallState(cursor.getString(cursor.getColumnIndex(CALL_STATE)));
                m.setCallImgPath(cursor.getString(cursor.getColumnIndex(CALL_IMG_PATH)));
                m.setCallerName(cursor.getString(cursor.getColumnIndex(CALLER_NAME)));
                m.setCallData(cursor.getString(cursor.getColumnIndex(CALL_DATA)));
                m.setCallReadState(cursor.getInt(cursor.getColumnIndex(CALL_READ_STATE)) != 0);
                list.add(m);
            }
        } catch (Exception e) {
            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
        }

        return list;
    }

    @SuppressLint("Range")
    public ArrayList<CallModel> getShowCalls() {
        ArrayList<CallModel> list = new ArrayList<>();

        MyUtils.getInstance().callsCount = (int) queryLong("SELECT COUNT(*) FROM " + TABLE_CALLS, null, 0);

        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT * FROM " + TABLE_CALLS +
                        " ORDER BY CAST(" + CALL_DATE + " AS INTEGER) DESC LIMIT 500",
                null
        )) {
            while (cursor.moveToNext()) {
                CallModel m = new CallModel();
                m.setCallId(cursor.getString(cursor.getColumnIndex(CALL_ID)));
                m.setCallFrom(cursor.getString(cursor.getColumnIndex(CALL_FROM)));
                m.setCallTo(cursor.getString(cursor.getColumnIndex(CALL_TO)));
                m.setCallType(cursor.getString(cursor.getColumnIndex(CALL_TYPE)));
                m.setCallDate(cursor.getString(cursor.getColumnIndex(CALL_DATE)));
                m.setCallState(cursor.getString(cursor.getColumnIndex(CALL_STATE)));
                m.setCallImgPath(cursor.getString(cursor.getColumnIndex(CALL_IMG_PATH)));
                m.setCallerName(cursor.getString(cursor.getColumnIndex(CALLER_NAME)));
                m.setCallData(cursor.getString(cursor.getColumnIndex(CALL_DATA)));
                m.setCallReadState(cursor.getInt(cursor.getColumnIndex(CALL_READ_STATE)) != 0);
                list.add(m);
            }
        } catch (Exception e) {
            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
        }

        return list;
    }

    public ArrayList<String> getAllCallsPath() {
        ArrayList<String> list = new ArrayList<>();
        String sql = "SELECT " + CALL_IMG_PATH + " FROM " + TABLE_CALLS +
                " WHERE " + CALL_IMG_PATH + " IS NOT NULL AND " + CALL_IMG_PATH + " != ''";

        try (Cursor cursor = getReadableDatabase().rawQuery(sql, null)) {
            while (cursor.moveToNext()) {
                list.add(cursor.getString(0));
            }
        } catch (Exception e) {
            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
        }
        return list;
    }

    public ArrayList<String> getDeleteCallsPathForFolderLimit() {
        ArrayList<String> list = new ArrayList<>();

        String sql = "SELECT " + CALL_IMG_PATH + " FROM " + TABLE_CALLS +
                " WHERE " + CALL_IMG_PATH + " IS NOT NULL AND " + CALL_IMG_PATH + " != ''" +
                " ORDER BY CAST(" + CALL_DATE + " AS INTEGER) ASC LIMIT 51";

        try (Cursor cursor = getReadableDatabase().rawQuery(sql, null)) {
            while (cursor.moveToNext()) {
                list.add(cursor.getString(0));
            }
        } catch (Exception e) {
            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
        }

        return list;
    }

    @SuppressLint("Range")
    public CallModel getCall(String callId) {
        CallModel callModel = new CallModel();

        try (Cursor cursor = getReadableDatabase().query(
                TABLE_CALLS,
                new String[]{CALL_ID, CALL_FROM, CALL_TO, CALL_TYPE, CALL_DATE, CALL_STATE, CALL_IMG_PATH, CALLER_NAME, CALL_DATA, CALL_READ_STATE},
                CALL_ID + "=?",
                new String[]{callId},
                null, null, null
        )) {
            if (cursor.moveToFirst()) {
                callModel.setCallId(cursor.getString(cursor.getColumnIndex(CALL_ID)));
                callModel.setCallFrom(cursor.getString(cursor.getColumnIndex(CALL_FROM)));
                callModel.setCallTo(cursor.getString(cursor.getColumnIndex(CALL_TO)));
                callModel.setCallType(cursor.getString(cursor.getColumnIndex(CALL_TYPE)));
                callModel.setCallDate(cursor.getString(cursor.getColumnIndex(CALL_DATE)));
                callModel.setCallState(cursor.getString(cursor.getColumnIndex(CALL_STATE)));
                callModel.setCallImgPath(cursor.getString(cursor.getColumnIndex(CALL_IMG_PATH)));
                callModel.setCallerName(cursor.getString(cursor.getColumnIndex(CALLER_NAME)));
                callModel.setCallData(cursor.getString(cursor.getColumnIndex(CALL_DATA)));
                callModel.setCallReadState(cursor.getInt(cursor.getColumnIndex(CALL_READ_STATE)) != 0);
            }
        } catch (Exception e) {
            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
        }

        return callModel;
    }

    public void deleteCall(String callID) {
        inTx(db -> db.delete(TABLE_CALLS, CALL_ID + " = ?", new String[]{callID}));
    }

    public void deleteCalls(ArrayList<CallModel> calls) {
        // Önce path’leri al
        ArrayList<String> paths = new ArrayList<>();
        for (CallModel c : calls) {
            if (c != null && c.getCallImgPath() != null) paths.add(c.getCallImgPath());
        }

        // Sonra DB sil
        inTx(db -> {
            for (CallModel model : calls) {
                db.delete(TABLE_CALLS, CALL_ID + " = ?", new String[]{model.getCallId()});
            }
        });

        // En son dosya sil
        for (String p : paths) {
            safeDeleteFile(p);
        }
    }

    public void deleteAllCalls() {
        ArrayList<String> paths = getAllCallsPath();

        inTx(db -> db.delete(TABLE_CALLS, null, null));

        for (String p : paths) {
            safeDeleteFile(p);
        }
    }

    // -------------------- IMAGES --------------------

    @SuppressLint("Range")
    public void insertImage(ImagesModel model) {
        long count = queryLong("SELECT COUNT(*) FROM " + TABLE_IMAGES, null, 0);

        inTx(db -> {
            if (count >= 500) {
                long delRowId = -1;
                String delPath = null;

                // imageID hiç set edilmiyor, bu yüzden rowid ile en eskiyi sil
                String q = "SELECT rowid, " + IMAGE_PATH +
                        " FROM " + TABLE_IMAGES +
                        " ORDER BY CAST(" + IMAGE_TIME + " AS INTEGER) ASC LIMIT 1";

                try (Cursor c = db.rawQuery(q, null)) {
                    if (c.moveToFirst()) {
                        delRowId = c.getLong(0);
                        delPath = c.getString(1);
                    }
                }

                if (delRowId != -1) {
                    db.delete(TABLE_IMAGES, "rowid=?", new String[]{String.valueOf(delRowId)});
                    safeDeleteFile(delPath);
                }
            }

            ContentValues cv = new ContentValues();
            cv.put(IMAGE_PATH, model.getPath());
            cv.put(IMAGE_TIME, model.getTime());
            cv.put(IMAGE_MAC_ADDRESS, model.getMacAddress());
            db.insert(TABLE_IMAGES, null, cv);
        });
    }

    @SuppressLint("Range")
    public ArrayList<ImagesModel> getAllImages() {
        ArrayList<ImagesModel> list = new ArrayList<>();

        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT * FROM " + TABLE_IMAGES +
                        " ORDER BY CAST(" + IMAGE_TIME + " AS INTEGER) DESC",
                null
        )) {
            while (cursor.moveToNext()) {
                // imageID boş kalabiliyor; UI için yine okuyalım
                long id = 0;
                try { id = cursor.getLong(cursor.getColumnIndex(IMAGE_ID)); } catch (Exception ignore) {}

                String time = cursor.getString(cursor.getColumnIndex(IMAGE_TIME));
                String path = cursor.getString(cursor.getColumnIndex(IMAGE_PATH));
                String catalog = cursor.getString(cursor.getColumnIndex(IMAGE_MAC_ADDRESS));
                list.add(new ImagesModel(id, path, time, catalog));
            }
        } catch (Exception e) {
            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
        }

        return list;
    }

    public ArrayList<String> getAllImagesPath() {
        ArrayList<String> list = new ArrayList<>();

        String sql = "SELECT " + IMAGE_PATH +
                " FROM " + TABLE_IMAGES +
                " WHERE " + IMAGE_PATH + " IS NOT NULL AND " + IMAGE_PATH + " != ''";

        try (Cursor cursor = getReadableDatabase().rawQuery(sql, null)) {
            while (cursor.moveToNext()) {
                list.add(cursor.getString(0));
            }
        } catch (Exception e) {
            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
        }

        return list;
    }

    public void deleteImages(String time) {
        inTx(db -> db.delete(TABLE_IMAGES, IMAGE_TIME + " = ?", new String[]{time}));
    }

    public void deleteAllImages() {
        ArrayList<String> paths = getAllImagesPath();

        inTx(db -> db.delete(TABLE_IMAGES, null, null));

        for (String p : paths) {
            try {
                File fDelete = new File(p);
                if (fDelete.exists()) {
                    boolean value = fDelete.delete();
                    Log.d("alfanar ", "[deleteAllImages] file not Deleted :" + p + " -- " + value);
                }
            } catch (Exception e) {
                Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
            }
        }
    }

    // -------------------- CAMERAS2 --------------------

    public ArrayList<Camera2Model> getAllCameras2() {
        ArrayList<Camera2Model> list = new ArrayList<>();

        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT * FROM " + TABLE_CAMERAS2 + " ORDER BY " + CAMERA_ID + " DESC",
                null
        )) {
            while (cursor.moveToNext()) {
                // Orijinal sıra ile uyumlu:
                int id = cursor.getInt(0);
                String name = cursor.getString(1);
                String ip = cursor.getString(2);
                String user = cursor.getString(3);
                String pass = cursor.getString(4);
                list.add(new Camera2Model(id, name, ip, user, pass));
            }
        } catch (Exception e) {
            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
        }

        return list;
    }

    public void deleteCamera2(int cam_id) {
        inTx(db -> db.delete(TABLE_CAMERAS2, CAMERA_ID + " = ?", new String[]{String.valueOf(cam_id)}));
    }

    public void insertCamera2(Camera2Model model) {
        inTx(db -> {
            ContentValues cv = new ContentValues();
            cv.put(CAMERA_NAME, model.getName());
            cv.put(CAMERA_IP, model.getIp());
            cv.put(CAMERA_USER, model.getUserName());
            cv.put(CAMERA_PASS, model.getPassword());
            db.insert(TABLE_CAMERAS2, null, cv);
        });
    }

    public void updateCamera2(Camera2Model model) {
        inTx(db -> {
            ContentValues cv = new ContentValues();
            cv.put(CAMERA_NAME, model.getName());
            cv.put(CAMERA_IP, model.getIp());
            cv.put(CAMERA_USER, model.getUserName());
            cv.put(CAMERA_PASS, model.getPassword());
            db.update(TABLE_CAMERAS2, cv, CAMERA_ID + "=?", new String[]{String.valueOf(model.getId())});
        });
    }

    // -------------------- CAMERAS (legacy) --------------------

    private void insertCamera(CameraDevice model) {
        inTx(db -> {
            ContentValues cv = new ContentValues();
            cv.put(CAMERA_ID, model.getId());
            cv.put(CAMERA_NAME, model.getName());
            cv.put(CAMERA_ADDRESS, model.getAddress());
            db.insert(TABLE_CAMERAS, null, cv);
        });
    }

    private void updateCamera(CameraDevice model) {
        inTx(db -> {
            ContentValues cv = new ContentValues();
            cv.put(CAMERA_NAME, model.getName());
            cv.put(CAMERA_ADDRESS, model.getAddress());
            db.update(TABLE_CAMERAS, cv, CAMERA_ID + "=?", new String[]{String.valueOf(model.getId())});
        });
    }

    private void deleteCamera(int cam_id) {
        inTx(db -> db.delete(TABLE_CAMERAS, CAMERA_ID + " = ?", new String[]{String.valueOf(cam_id)}));
    }

    @SuppressLint("Range")
    private ArrayList<CameraDevice> getAllCameras() {
        ArrayList<CameraDevice> list = new ArrayList<>();

        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT * FROM " + TABLE_CAMERAS + " ORDER BY " + CAMERA_ID + " ASC",
                null
        )) {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndex(CAMERA_ID));
                String name = cursor.getString(cursor.getColumnIndex(CAMERA_NAME));
                String addr = cursor.getString(cursor.getColumnIndex(CAMERA_ADDRESS));
                list.add(new CameraDevice(id, name, addr));
            }
        } catch (Exception e) {
            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
        }

        return list;
    }
}
