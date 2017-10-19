package fr.insensa.photoepsi.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by Jérôme on 16/03/2015.
 */
public class AccessDatabase {
    // ArrayListe qui contiendront les valeurs de l'ensemble de la base (requête sur toutes les lignes)
    private ArrayList<Integer> key_idArray = new ArrayList<>();
    private ArrayList<String> titreArray = new ArrayList<>();
    private ArrayList<String> CommentaireArray = new ArrayList<>();
    private ArrayList<String> localisationArray = new ArrayList<>();
    private ArrayList<String> uri_pictureArray = new ArrayList<>();
    private ArrayList<String> dateArray = new ArrayList<>();
    private ArrayList<String> authorArray = new  ArrayList<>();
    private ArrayList<String> orientationArray = new ArrayList<>();
    private ArrayList<Double> lattitudeArray = new ArrayList<>();
    private ArrayList<Double> longitudeArray = new ArrayList<>();

    private int countCursorLine=0;

    // utilisé quand requête porte sur une seule ligne de la base
    private Integer key_id;
    private String titre;
    private String commentaire;
    private String localisation;
    private String uri_picture;
    private String date;
    private String author;
    private String orientation;
    private Double lattitude;
    private Double longitude;


    //The index (key) column name for use in where clauses.
    // Table parcours
    private static final String KEY_ID = "_id";
    //The name and column index of each column in your database.
    //These should be descriptive.
    private static final String KEY_TITRE_PHOTO =
            "TITRE_PHOTO";
    private static final String KEY_COMMENTAIRE_PHOTO =
            "COMMENTAIRE_PHOTO";
    private static final String KEY_LOCALISATION =
            "LOCALISATION";
    private static final String KEY_URI_PICTURE =
            "URI_PICTURE";
    private static final String DATE =
            "DATE";
    private static final String AUTHOR =
            "AUTHOR";
    private static final String LATTITUDE =
            "LATTITUDE";
    private static final String LONGITUDE =
            "LONGITUDE";
    private static final String ORIENTATION =
            "ORIENTATION";

    // Database open/upgrade helper
    private static AccessDBOpenHelper accessDBOpenHelper;

    public AccessDatabase(Context context) {
        accessDBOpenHelper = new AccessDBOpenHelper(context, AccessDBOpenHelper.DATABASE_NAME, null,
                AccessDBOpenHelper.DATABASE_VERSION);
        System.out.println("accessDataBase initialized");
    }

    // Called when you no longer need access to the database.
    public void closeDatabase() {
        accessDBOpenHelper.close();
    }


    // Pour obtenir le Cursor de l'ensembles des photos (soit toutes les lignes de la base
    private Cursor getAllPictures() {

        // Les colonnes que l'on souhaite extraire

        String[] result_columns = new String[] {
                KEY_ID, KEY_TITRE_PHOTO, KEY_LOCALISATION, KEY_COMMENTAIRE_PHOTO,
                KEY_URI_PICTURE, DATE, AUTHOR, ORIENTATION, LATTITUDE, LONGITUDE};
        // les clauses sql pour limiter ou formater le rendu
        String where = null ;
        String whereArgs[] = null;
        String groupBy = null;
        String having = null;
        String order = KEY_ID + " ASC";

        SQLiteDatabase db = accessDBOpenHelper.getWritableDatabase();
        Cursor cursor = db.query(AccessDBOpenHelper.DATABASE_TABLE,
                result_columns, where,
                whereArgs, groupBy, having, order);
        //
        return cursor;
    }

    // exemple pour lire le cursor de tous les photos
    public void getCursorValues() {
        // récupération du cursor de tous les photos
        Cursor cursor = getAllPictures();
        // on se positionne sur la première ligne
        cursor.moveToFirst();
        // on vérfie qu'on a pas déjà atteint la dernière ligne
        while (!cursor.isAfterLast()) {
            // récupération des valeurs de la ligne sur laquelle on est positionné et à la colonne indiquée en index
            Integer key_idCursor = cursor.getInt(0);
            String titreCursor = cursor.getString(1);
            String localisationCursor= cursor.getString(2);
            String commentaireCursor = cursor.getString(3);
            String uri_pictureCursor = cursor.getString(4);
            String dateCursor = cursor.getString(5);
            String authorCursor = cursor.getString(6);
            String orientationCursor = cursor.getString(7);
            Double lattitudeCursor = cursor.getDouble(8);
            Double longitudeCursor = cursor.getDouble(9);

            key_idArray.add(key_idCursor);
            titreArray.add(titreCursor);
            localisationArray.add(localisationCursor);
            CommentaireArray.add(commentaireCursor);
            uri_pictureArray.add(uri_pictureCursor);
            dateArray.add(dateCursor);
            authorArray.add(authorCursor);
            orientationArray.add(orientationCursor);
            lattitudeArray.add(lattitudeCursor);
            longitudeArray.add(longitudeCursor);

            // aller à la prochaine ligne
            cursor.moveToNext();
        }
        // pour compter le nombre de ligne, utile dans méthode main pour créer des vues de rendu en fonction
        countCursorLine = cursor.getCount();
        // libération des ressources occupées par le curseur.
        cursor.close();
    }


    // Pour obtenir le Cursor des éléments liés à la liste des parcours d'un parcours
    public void getOneCursorValues(String pIdPhoto) {

        // récupération du cursor d'une photo
        Cursor cursor = getOnePicture(pIdPhoto);
        // on se positionne sur la première ligne
        cursor.moveToFirst();
        // on vérfie qu'on a pas déjà atteint la dernière ligne

        // récupération des valeurs de la ligne sur laquelle on est positionné et à la colonne indiquée en index
        Integer key_idCursor = cursor.getInt(0);
        String titreCursor = cursor.getString(1);
        String localisationCursor= cursor.getString(2);
        String commentaireCursor = cursor.getString(3);
        String uri_pictureCursor = cursor.getString(4);
        String dateCursor = cursor.getString(5);
        String authorCursor = cursor.getString(6);
        String orientationCursor = cursor.getString(7);
        Double lattitudeCursor = cursor.getDouble(8);
        Double longitudeCursor = cursor.getDouble(9);

        key_id = key_idCursor;
        titre = titreCursor;
        localisation = localisationCursor;
        commentaire = commentaireCursor;
        uri_picture = uri_pictureCursor;
        date = dateCursor;
        author = authorCursor;
        orientation = orientationCursor;
        lattitude = lattitudeCursor;
        longitude = longitudeCursor;


        // pour compter le nombre de ligne, utile dans méthode main pour créer des vues de rendu en fonction
        countCursorLine = cursor.getCount();

        // libération des ressources occupées par le curseur.
        cursor.close();
    }

    // pour obtenir le Cursor du dernier élément rentré en BD
    public void getLastCursorValue()
    {
        // récupération du cursor de tous les photos
        Cursor cursor = getAllPictures();
        // on se positionne sur la première ligne
        cursor.moveToLast();

            // récupération des valeurs de la ligne sur laquelle on est positionné et à la colonne indiquée en index
            Integer key_idCursor = cursor.getInt(0);
            String titreCursor = cursor.getString(1);
            String localisationCursor= cursor.getString(2);
            String commentaireCursor = cursor.getString(3);
            String uri_pictureCursor = cursor.getString(4);
            String dateCursor = cursor.getString(5);
            String authorCursor = cursor.getString(6);
            String orientationCursor = cursor.getString(7);
            Double lattitudeCursor = cursor.getDouble(8);
            Double longitudeCursor = cursor.getDouble(9);

            key_id = key_idCursor;
            titre = titreCursor;
            localisation = localisationCursor;
            commentaire = commentaireCursor;
            uri_picture = uri_pictureCursor;
            date = dateCursor;
            author = authorCursor;
            orientation = orientationCursor;
            lattitude = lattitudeCursor;
            longitude = longitudeCursor;


        // pour compter le nombre de ligne, utile dans méthode main pour créer des vues de rendu en fonction
        countCursorLine = cursor.getCount();
        // libération des ressources occupées par le curseur.
        cursor.close();
    }

    private Cursor getOnePicture(String pIdPhoto) {

        // Les colonnes que l'on souhaite extraire

        String[] result_columns = new String[] {
                KEY_ID, KEY_TITRE_PHOTO, KEY_LOCALISATION, KEY_COMMENTAIRE_PHOTO,
                KEY_URI_PICTURE, DATE, AUTHOR, ORIENTATION, LATTITUDE, LONGITUDE};

        // La clause Where doit être renseignée si l'on veut limiter les lignes
        String where = KEY_ID  + "=\"" + pIdPhoto + "\"" ;

        //remplace éventuellement la clause where = KEY_ID + "=?"
        String whereArgs[] = null;
        String groupBy = null;
        String having = null;
        String order = null;

        SQLiteDatabase db = accessDBOpenHelper.getWritableDatabase();
        Cursor cursor = db.query(AccessDBOpenHelper.DATABASE_TABLE,
                result_columns, where,
                whereArgs, groupBy, having, order);
        //
        return cursor;
    }

    // méthodes de récupération des données du tableau
    // test préalbale que le tableau n'est pas vide
    public Integer getKeyId(int i){
        if (key_idArray.size()==0)
            return null;
        return key_idArray.get(i);
    }
    public String getTitre(int i){
        if (titreArray.size()==0)
            return null;
        return titreArray.get(i);
    }
    public String getCommentaires(int i){
        if (CommentaireArray.size()==0)
            return null;
        return CommentaireArray.get(i);
    }
    public String getLocalisation(int i){
        if (localisationArray.size()==0)
            return null;
        else return localisationArray.get(i);
    }
    public String getUri_picture(int i){
        if (uri_pictureArray.size()==0)
            return null;
        return uri_pictureArray.get(i);
    }
    public String getDate(int i){
        if (dateArray.size()==0)
            return null;
        return dateArray.get(i);
    }

    public  String getAuthor(int i) {
        if (authorArray.size()==0)
            return null;
        return  authorArray.get(i);
    }

    public  String getOrientation(int i) {
        if (orientationArray.size()==0)
            return null;
        return  orientationArray.get(i);
    }
    public  Double getLattitude(int i) {
        if (lattitudeArray.size()==0)
            return null;
        return  lattitudeArray.get(i);
    }
    public  Double getLongitude(int i) {
        if (longitudeArray.size()==0)
            return null;
        return  longitudeArray.get(i);
    }


    public int getCountCursorLine(){
        return countCursorLine;
    }

    // méthodes de récupération des données des variables quand une seule photo est interrogée
    public Integer getOneKeyId(){
        return key_id;
    }
    public String getOneTitre(){
        return titre;
    }
    public String getOneCommentaire(){
        return commentaire;
    }
    public String getOneLocalisation(){
        return localisation;
    }
    public String getOneUri_picture(){
        return uri_picture;
    }
    public String getOneDate(){
        return date;
    }
    public String getOneAuthor() {
        return author;
    }
    public String getOneOrientation() {
        return orientation;
    }
    public Double getOneLattitude() {
        return lattitude;
    }
    public Double getOneLongitude() {
        return longitude;
    }


    // ajouter une entrées dans la base (ici nouvelles photos)
    public void addNewPicture(String pTitre, String pLocalisation, String pCommentaire, String pUri_picture, String pDate,
                              String pAuthor, String pOrientation, Double pLattitude, Double pLongitude) {
        System.out.println("debut addPicture");
        // Create a new row of values to insert.
        ContentValues newValues = new ContentValues();
        // Assign values for each row.
        newValues.put(KEY_TITRE_PHOTO, pTitre);
        newValues.put(KEY_LOCALISATION, pLocalisation);
        newValues.put(KEY_COMMENTAIRE_PHOTO, pCommentaire);
        newValues.put(KEY_URI_PICTURE, pUri_picture);
        newValues.put(DATE, pDate);
        newValues.put(AUTHOR, pAuthor);
        newValues.put(ORIENTATION, pOrientation);
        newValues.put(LATTITUDE, pLattitude);
        newValues.put(LONGITUDE, pLongitude);

        // Insert the row into your table
        SQLiteDatabase db = accessDBOpenHelper.getWritableDatabase();
        db.insert(AccessDBOpenHelper.DATABASE_TABLE, null, newValues);
        System.out.println("addPictureFinish");
    }


    // mettre à jour les données d'un parcours
    public void updatePictureValue(String pIdPhoto, String pTitre, String pCommentaire) {

        // Create the updated row Content Values.
        ContentValues updatedValues = new ContentValues();

        // Assign values for each row.
        updatedValues.put(KEY_TITRE_PHOTO, pTitre);
        updatedValues.put(KEY_COMMENTAIRE_PHOTO, pCommentaire);

        // Specify a where clause the defines which rows should be
        // updated. Specify where arguments as necessary.
        String where = KEY_ID + "=\"" + pIdPhoto + "\"" ;
        String whereArgs[] = null;

        // Update the row with the specified index with the new values.
        SQLiteDatabase db = accessDBOpenHelper.getWritableDatabase();
        db.update(AccessDBOpenHelper.DATABASE_TABLE, updatedValues,
                where, whereArgs);
    }


    // pour effacer des lignes d'une table (ici toutes les photos)
    public void deletePictures() {
        // Specify a where clause that determines which row(s) to delete as necessary
        String where = null;
        String whereArgs[] = null;

        // Delete the rows that match the where clause.
        SQLiteDatabase db = accessDBOpenHelper.getWritableDatabase();
        db.delete(AccessDBOpenHelper.DATABASE_TABLE, where, whereArgs);
    }

    /**
     * Listing 8-2: Implementing an SQLite Open Helper
     */
    private static class AccessDBOpenHelper extends SQLiteOpenHelper {


        private static final String DATABASE_NAME = "PhotoPartageDatabase.db";
        private static final String DATABASE_TABLE = "Photos";
        private static final int DATABASE_VERSION = 1;

        // SQL Statement to create the new table of parcours in database.
        private static final String DATABASE_CREATE = "create table " +
                DATABASE_TABLE + " (" + KEY_ID +
                " integer primary key autoincrement, " +
                KEY_TITRE_PHOTO + " varchar(255), " +
                KEY_LOCALISATION + " varchar(255), " +
                KEY_COMMENTAIRE_PHOTO + " varchar(500), " +
                KEY_URI_PICTURE + " varchar(255), " +
                DATE + " varchar(255), " +
                AUTHOR + " varchar(255), " +
                ORIENTATION + " varchar(255), " +
                LATTITUDE + " varchar(255), " +
                LONGITUDE + " varchar(255)" + ");";

        public AccessDBOpenHelper(Context context, String name,
                                  SQLiteDatabase.CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        // Called when no database exists in disk and the helper class needs
        // to create a new one.
        @Override
        public void onCreate(SQLiteDatabase db) {
            // création table Photo
            db.execSQL(DATABASE_CREATE);
        }


        // Called when there is a database version mismatch meaning that
        // the version of the database on disk needs to be upgraded to
        // the current version.
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion,
                              int newVersion) {
            // Log the version upgrade.
            Log.w("TaskDBAdapter", "Upgrading from version " +
                    oldVersion + " to " +
                    newVersion + ", which will destroy all old data");

            // Upgrade the existing database to conform to the new
            // version. Multiple previous versions can be handled by
            // comparing oldVersion and newVersion values.

            // The simplest case is to drop the old table and create a new one.
            db.execSQL("DROP TABLE IF IT EXISTS " + DATABASE_TABLE);
            // Create a new one.
            onCreate(db);
        }
    }
}
