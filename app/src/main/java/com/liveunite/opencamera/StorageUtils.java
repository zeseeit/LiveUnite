package com.liveunite.opencamera;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore.Images;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

//import android.content.ContentValues;
//import android.location.Location;

/**
 * Provides access to the filesystem. Supports both standard and Storage
 * Access Framework.
 */
public class StorageUtils
{
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    private static final String TAG = "StorageUtils";
    // for testing:
    public boolean failed_to_scan = false;
    Context context = null;
    private Uri last_media_scanned = null;
    static File baseDir;

    StorageUtils(Context context)
    {
        this.context = context;
        baseDir = this.context.getExternalFilesDir(null);
        //baseDir = context.getFilesDir();
    }

    public static File getBaseFolder()
    {
        return baseDir;
        //return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
    }

    public static File getImageFolder(String folder_name)
    {
        File file = null;
        if (folder_name.length() > 0 && folder_name.lastIndexOf('/') == folder_name.length() - 1)
        {
            // ignore final '/' character
            folder_name = folder_name.substring(0, folder_name.length() - 1);
        }
        //if( folder_name.contains("/") ) {
        if (folder_name.startsWith("/"))
        {
            file = new File(folder_name);
        } else
        {
            file = new File(getBaseFolder(), folder_name);
        }
        return file;
    }

    Uri getLastMediaScanned()
    {
        return last_media_scanned;
    }

	/*public Uri broadcastFileRaw(File file, Date current_date, Location location) {
		if( MyDebug.LOG )
			Log.d(TAG, "broadcastFileRaw: " + file.getAbsolutePath());
        ContentValues values = new ContentValues(); 
        values.put(ImageColumns.TITLE, file.getName().substring(0, file.getName().lastIndexOf(".")));
        values.put(ImageColumns.DISPLAY_NAME, file.getName());
        values.put(ImageColumns.DATE_TAKEN, current_date.getTime()); 
        values.put(ImageColumns.MIME_TYPE, "image/dng");
        //values.put(ImageColumns.MIME_TYPE, "image/jpeg");
        if( location != null ) {
            values.put(ImageColumns.LATITUDE, location.getLatitude());
            values.put(ImageColumns.LONGITUDE, location.getLongitude());
        }
        // leave ORIENTATION for now - this doesn't seem to get inserted for JPEGs anyway (via MediaScannerConnection.scanFile())
        values.put(ImageColumns.DATA, file.getAbsolutePath());
        //values.put(ImageColumns.DATA, "/storage/emulated/0/DCIM/OpenCamera/blah.dng");
        Uri uri = null;
        try {
    		uri = context.getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values); 
 			if( MyDebug.LOG )
 				Log.d(TAG, "inserted media uri: " + uri);
    		context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
        }
        catch (Throwable th) { 
	        // This can happen when the external volume is already mounted, but 
	        // MediaScanner has not notify MediaProvider to add that volume. 
	        // The picture is still safe and MediaScanner will find it and 
	        // insert it into MediaProvider. The only problem is that the user 
	        // cannot click the thumbnail to review the picture. 
	        Log.e(TAG, "Failed to write MediaStore" + th); 
	    }
        return uri;
	}*/

    void clearLastMediaScanned()
    {
        last_media_scanned = null;
    }

    /**
     * Sends the intents to announce the new file to other Android applications. E.g., cloud storage applications like
     * OwnCloud use this to listen for new photos/videos to automatically upload.
     */
    void announceUri(Uri uri, boolean is_new_picture, boolean is_new_video)
    {
        if (MyDebug.LOG)
            Log.d(TAG, "announceUri: " + uri);
        if (is_new_picture)
        {
            // note, we reference the string directly rather than via Camera.ACTION_NEW_PICTURE, as the latter class is now deprecated - but we still need to broadcast the string for other apps
            context.sendBroadcast(new Intent("android.hardware.action.NEW_PICTURE", uri));
            // for compatibility with some apps - apparently this is what used to be broadcast on Android?
            context.sendBroadcast(new Intent("com.android.camera.NEW_PICTURE", uri));

            if (MyDebug.LOG) // this code only used for debugging/logging
            {
                String[] CONTENT_PROJECTION = {Images.Media.DATA, Images.Media.DISPLAY_NAME, Images.Media.MIME_TYPE, Images.Media.SIZE, Images.Media.DATE_TAKEN, Images.Media.DATE_ADDED};
                Cursor c = context.getContentResolver().query(uri, CONTENT_PROJECTION, null, null, null);
                if (c == null)
                {
                    if (MyDebug.LOG)
                        Log.e(TAG, "Couldn't resolve given uri [1]: " + uri);
                } else if (!c.moveToFirst())
                {
                    if (MyDebug.LOG)
                        Log.e(TAG, "Couldn't resolve given uri [2]: " + uri);
                } else
                {
                    String file_path = c.getString(c.getColumnIndex(Images.Media.DATA));
                    String file_name = c.getString(c.getColumnIndex(Images.Media.DISPLAY_NAME));
                    String mime_type = c.getString(c.getColumnIndex(Images.Media.MIME_TYPE));
                    long date_taken = c.getLong(c.getColumnIndex(Images.Media.DATE_TAKEN));
                    long date_added = c.getLong(c.getColumnIndex(Images.Media.DATE_ADDED));
                    Log.d(TAG, "file_path: " + file_path);
                    Log.d(TAG, "file_name: " + file_name);
                    Log.d(TAG, "mime_type: " + mime_type);
                    Log.d(TAG, "date_taken: " + date_taken);
                    Log.d(TAG, "date_added: " + date_added);
                    c.close();
                }
            }
             /*{
 				// hack: problem on Camera2 API (at least on Nexus 6) that if geotagging is enabled, then the resultant image has incorrect Exif TAG_GPS_DATESTAMP (GPSDateStamp) set (tends to be around 2038 - possibly a driver bug of casting long to int?)
 				// whilst we don't yet correct for that bug, the more immediate problem is that it also messes up the DATE_TAKEN field in the media store, which messes up Gallery apps
 				// so for now, we correct it based on the DATE_ADDED value.
    	        String[] CONTENT_PROJECTION = { Images.Media.DATE_ADDED };
    	        Cursor c = context.getContentResolver().query(uri, CONTENT_PROJECTION, null, null, null);
    	        if( c == null ) {
		 			if( MyDebug.LOG )
		 				Log.e(TAG, "Couldn't resolve given uri [1]: " + uri);
    	        }
    	        else if( !c.moveToFirst() ) {
		 			if( MyDebug.LOG )
		 				Log.e(TAG, "Couldn't resolve given uri [2]: " + uri);
    	        }
    	        else {
        	        long date_added = c.getLong(c.getColumnIndex(Images.Media.DATE_ADDED));
		 			if( MyDebug.LOG )
		 				Log.e(TAG, "replace date_taken with date_added: " + date_added);
					ContentValues values = new ContentValues();
					values.put(Images.Media.DATE_TAKEN, date_added*1000);
					context.getContentResolver().update(uri, values, null, null);
        	        c.close();
    	        }
 			}*/
        } else if (is_new_video)
        {
            context.sendBroadcast(new Intent("android.hardware.action.NEW_VIDEO", uri));

    		/*String[] CONTENT_PROJECTION = { Video.Media.DURATION };
	        Cursor c = context.getContentResolver().query(uri, CONTENT_PROJECTION, null, null, null);
	        if( c == null ) {
	 			if( MyDebug.LOG )
	 				Log.e(TAG, "Couldn't resolve given uri [1]: " + uri);
	        }
	        else if( !c.moveToFirst() ) {
	 			if( MyDebug.LOG )
	 				Log.e(TAG, "Couldn't resolve given uri [2]: " + uri);
	        }
	        else {
    	        long duration = c.getLong(c.getColumnIndex(Video.Media.DURATION));
	 			if( MyDebug.LOG )
	 				Log.e(TAG, "replace duration: " + duration);
				ContentValues values = new ContentValues();
				values.put(Video.Media.DURATION, 1000);
				context.getContentResolver().update(uri, values, null, null);
    	        c.close();
	        }*/
        }
    }

    /**
     * Sends a "broadcast" for the new file. This is necessary so that Android recognises the new file without needing a reboot:
     * - So that they show up when connected to a PC using MTP.
     * - For JPEGs, so that they show up in gallery applications.
     * - This also calls announceUri() on the resultant Uri for the new file.
     * - Note this should also be called after deleting a file.
     * - Note that for DNG files, MediaScannerConnection.scanFile() doesn't result in the files being shown in gallery applications.
     * This may well be intentional, since most gallery applications won't read DNG files anyway. But it's still important to
     * call this function for DNGs, so that they show up on MTP.
     */
    public void broadcastFile(final File file, final boolean is_new_picture, final boolean is_new_video, final boolean set_last_scanned)
    {
        if (MyDebug.LOG)
            Log.d(TAG, "broadcastFile: " + file.getAbsolutePath());
        // note that the new method means that the new folder shows up as a file when connected to a PC via MTP (at least tested on Windows 8)
        if (file.isDirectory())
        {
            //this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.fromFile(file)));
            // ACTION_MEDIA_MOUNTED no longer allowed on Android 4.4! Gives: SecurityException: Permission Denial: not allowed to send broadcast android.intent.action.MEDIA_MOUNTED
            // note that we don't actually need to broadcast anything, the folder and contents appear straight away (both in Gallery on device, and on a PC when connecting via MTP)
            // also note that we definitely don't want to broadcast ACTION_MEDIA_SCANNER_SCAN_FILE or use scanFile() for folders, as this means the folder shows up as a file on a PC via MTP (and isn't fixed by rebooting!)
        } else
        {
            // both of these work fine, but using MediaScannerConnection.scanFile() seems to be preferred over sending an intent
            //context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
            if(is_new_picture)
            {

            }
            else if(is_new_video)
            {
                /*
                Intent mIntent = new Intent(context, PicturePreview.class);
                mIntent.putExtra(Constant.PREVIEW_FILENAME,file.getAbsolutePath());
                mIntent.putExtra(Constant.UPLOAD_TYPE,Constant.TYPE_VIDEO);
                mIntent.putExtra(Constant.UPLOAD_AUTODELETE,true);
                context.startActivity(mIntent);
                */
            }
        }
    }

    boolean isUsingSAF()
    {
        // check Android version just to be safe
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            if (sharedPreferences.getBoolean(PreferenceKeys.getUsingSAFPreferenceKey(), false))
            {
                return true;
            }
        }
        return false;
    }

    // only valid if !isUsingSAF()
    String getSaveLocation()
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String folder_name = sharedPreferences.getString(PreferenceKeys.getSaveLocationPreferenceKey(), "OpenCamera");
        return folder_name;
    }

    // only valid if isUsingSAF()
    String getSaveLocationSAF()
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String folder_name = sharedPreferences.getString(PreferenceKeys.getSaveLocationSAFPreferenceKey(), "");
        return folder_name;
    }

    // only valid if isUsingSAF()
    Uri getTreeUriSAF()
    {
        String folder_name = getSaveLocationSAF();
        Uri treeUri = Uri.parse(folder_name);
        return treeUri;
    }

    /**
     * Returns a human readable name for the current SAF save folder location.
     * Only valid if isUsingSAF().
     *
     * @return The human readable form. This will be null if the Uri is not recognised.
     */
    // only valid if isUsingSAF()
    // return a human readable name for the SAF save folder location
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    String getImageFolderNameSAF()
    {
        if (MyDebug.LOG)
            Log.d(TAG, "getImageFolderNameSAF");
        Uri uri = getTreeUriSAF();
        if (MyDebug.LOG)
            Log.d(TAG, "uri: " + uri);
        return getImageFolderNameSAF(uri);
    }

    /**
     * Returns a human readable name for a SAF save folder location.
     * Only valid if isUsingSAF().
     *
     * @param folder_name The SAF uri for the requested save location.
     * @return The human readable form. This will be null if the Uri is not recognised.
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    String getImageFolderNameSAF(Uri folder_name)
    {
        if (MyDebug.LOG)
            Log.d(TAG, "getImageFolderNameSAF: " + folder_name);
        String filename = null;
        if ("com.android.externalstorage.documents".equals(folder_name.getAuthority()))
        {
            final String id = DocumentsContract.getTreeDocumentId(folder_name);
            if (MyDebug.LOG)
                Log.d(TAG, "id: " + id);
            String[] split = id.split(":");
            if (split.length >= 2)
            {
                String type = split[0];
                String path = split[1];
                if (MyDebug.LOG)
                {
                    Log.d(TAG, "type: " + type);
                    Log.d(TAG, "path: " + path);
                }
                filename = path;
            }
        }
        return filename;
    }

    // only valid if isUsingSAF()
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private File getFileFromDocumentIdSAF(String id)
    {
        File file = null;
        String[] split = id.split(":");
        if (split.length >= 2)
        {
            String type = split[0];
            String path = split[1];
    		/*if( MyDebug.LOG ) {
    			Log.d(TAG, "type: " + type);
    			Log.d(TAG, "path: " + path);
    		}*/
            File[] storagePoints = new File("/storage").listFiles();

            if ("primary".equalsIgnoreCase(type))
            {
                final File externalStorage = Environment.getExternalStorageDirectory();
                file = new File(externalStorage, path);
            }
            for (int i = 0; storagePoints != null && i < storagePoints.length && file == null; i++)
            {
                File externalFile = new File(storagePoints[i], path);
                if (externalFile.exists())
                {
                    file = externalFile;
                }
            }
        }
        return file;
    }

    // valid if whether or not isUsingSAF()
    // but note that if isUsingSAF(), this may return null - it can't be assumed that there is a File corresponding to the SAF Uri
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    File getImageFolder()
    {
        File file = null;
        if (isUsingSAF())
        {
            Uri uri = getTreeUriSAF();
    		/*if( MyDebug.LOG )
    			Log.d(TAG, "uri: " + uri);*/
            if ("com.android.externalstorage.documents".equals(uri.getAuthority()))
            {
                final String id = DocumentsContract.getTreeDocumentId(uri);
        		/*if( MyDebug.LOG )
        			Log.d(TAG, "id: " + id);*/
                file = getFileFromDocumentIdSAF(id);
            }
        } else
        {
            //String folder_name = getSaveLocation();
            //file = getImageFolder(folder_name);
            file = getBaseFolder();
        }
        return file;
    }

    // only valid if isUsingSAF()
    // This function should only be used as a last resort - we shouldn't generally assume that a Uri represents an actual File, and instead.
    // However this is needed for a workaround to the fact that deleting a document file doesn't remove it from MediaStore.
    // See:
    // http://stackoverflow.com/questions/21605493/storage-access-framework-does-not-update-mediascanner-mtp
    // http://stackoverflow.com/questions/20067508/get-real-path-from-uri-android-kitkat-new-storage-access-framework/
    // only valid if isUsingSAF()
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    File getFileFromDocumentUriSAF(Uri uri)
    {
        if (MyDebug.LOG)
            Log.d(TAG, "getFileFromDocumentUriSAF: " + uri);
        File file = null;
        if ("com.android.externalstorage.documents".equals(uri.getAuthority()))
        {
            final String id = DocumentsContract.getDocumentId(uri);
            if (MyDebug.LOG)
                Log.d(TAG, "id: " + id);
            file = getFileFromDocumentIdSAF(id);
        }
        if (MyDebug.LOG)
        {
            if (file != null)
                Log.d(TAG, "file: " + file.getAbsolutePath());
            else
                Log.d(TAG, "failed to find file");
        }
        return file;
    }

    private String createMediaFilename(int type, String suffix, int count, String extension, Date current_date)
    {
        String index = "";
        if (count > 0)
        {
            index = "_" + count; // try to find a unique filename
        }
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean useZuluTime = sharedPreferences.getString(PreferenceKeys.getSaveZuluTimePreferenceKey(), "local").equals("zulu");
        String timeStamp = null;
        if (useZuluTime)
        {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd_HHmmss'Z'", Locale.US);
            fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
            timeStamp = fmt.format(current_date);
        } else
        {
            timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(current_date);
        }
        String mediaFilename = null;
        if (type == MEDIA_TYPE_IMAGE)
        {
            String prefix = sharedPreferences.getString(PreferenceKeys.getSavePhotoPrefixPreferenceKey(), "IMG_");
            mediaFilename = prefix + timeStamp + suffix + index + "." + extension;
        } else if (type == MEDIA_TYPE_VIDEO)
        {
            String prefix = sharedPreferences.getString(PreferenceKeys.getSaveVideoPrefixPreferenceKey(), "VID_");
            mediaFilename = prefix + timeStamp + suffix + index + "." + extension;
        } else
        {
            // throw exception as this is a programming error
            if (MyDebug.LOG)
                Log.e(TAG, "unknown type: " + type);
            throw new RuntimeException();
        }
        return mediaFilename;
    }

    // only valid if !isUsingSAF()
    @SuppressLint("SimpleDateFormat")
    File createOutputMediaFile(int type, String suffix, String extension, Date current_date) throws IOException
    {
        File mediaStorageDir = getImageFolder();

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists())
        {
            if (!mediaStorageDir.mkdirs())
            {
                if (MyDebug.LOG)
                    Log.e(TAG, "failed to create directory");
                throw new IOException();
            }
            broadcastFile(mediaStorageDir, false, false, false);
        }

        // Create a media file name
        File mediaFile = null;
        for (int count = 0; count < 100; count++)
        {
            String mediaFilename = createMediaFilename(type, suffix, count, extension, current_date);
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + mediaFilename);
            if (!mediaFile.exists())
            {
                break;
            }
        }

        if (MyDebug.LOG)
        {
            Log.d(TAG, "getOutputMediaFile returns: " + mediaFile);
        }
        if (mediaFile == null)
            throw new IOException();
        return mediaFile;
    }

    // only valid if isUsingSAF()
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    Uri createOutputMediaFileSAF(int type, String suffix, String extension, Date current_date) throws IOException
    {
        try
        {
            Uri treeUri = getTreeUriSAF();
            if (MyDebug.LOG)
                Log.d(TAG, "treeUri: " + treeUri);
            Uri docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri));
            if (MyDebug.LOG)
                Log.d(TAG, "docUri: " + docUri);
            String mimeType = "";
            if (type == MEDIA_TYPE_IMAGE)
            {
                if (extension.equals("dng"))
                {
                    mimeType = "image/dng";
                    //mimeType = "image/x-adobe-dng";
                } else
                    mimeType = "image/jpeg";
            } else if (type == MEDIA_TYPE_VIDEO)
            {
                mimeType = "video/mp4";
            } else
            {
                // throw exception as this is a programming error
                if (MyDebug.LOG)
                    Log.e(TAG, "unknown type: " + type);
                throw new RuntimeException();
            }
            // note that DocumentsContract.createDocument will automatically append to the filename if it already exists
            String mediaFilename = createMediaFilename(type, suffix, 0, extension, current_date);
            Uri fileUri = DocumentsContract.createDocument(context.getContentResolver(), docUri, mimeType, mediaFilename);
            if (MyDebug.LOG)
                Log.d(TAG, "returned fileUri: " + fileUri);
            if (fileUri == null)
                throw new IOException();
            return fileUri;
        } catch (IllegalArgumentException e)
        {
            // DocumentsContract.getTreeDocumentId throws this if URI is invalid
            if (MyDebug.LOG)
                Log.e(TAG, "createOutputMediaFileSAF failed");
            e.printStackTrace();
            throw new IOException();
        }
    }

    static class Media
    {
        long id;
        boolean video;
        Uri uri;
        long date;
        int orientation;

        Media(long id, boolean video, Uri uri, long date, int orientation)
        {
            this.id = id;
            this.video = video;
            this.uri = uri;
            this.date = date;
            this.orientation = orientation;
        }
    }
}
