package com.wudi.facegate.greenDao;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.github.yuweiguocn.library.greendao.MigrationHelper;

import org.greenrobot.greendao.query.QueryBuilder;

import java.util.List;

/**
 * greenDao操作管理类
 * Created by wudi on 2020/3/3.
 */
public class GreenDaoManager {
    private static DaoSession daoSession;

    /**
     * 初始化GreenDao,直接在Application中进行初始化操作
     */
    public static void initGreenDao(Context context){
        MigrationHelper.DEBUG = true;//数据库升级日志
        GreenDaoUpGradeHelper helper = new GreenDaoUpGradeHelper(context, "FaceGATE.db");
        SQLiteDatabase db = helper.getWritableDatabase();
        DaoMaster daoMaster = new DaoMaster(db);
        daoSession = daoMaster.newSession();
    }

    /**
     * 检查静态变量是否被回收   如果为空则重新创建
     * @param context
     */
    private static void checkDaoSession(Context context){
        if (daoSession == null){
            initGreenDao(context);
        }
    }

    /**
     * 获取数据库人员列表长度
     *
     * @return
     */
    public static long getPersonListCount(Context context) {
        checkDaoSession(context);
        QueryBuilder<Person> queryBuilder = daoSession.queryBuilder(Person.class);
        return queryBuilder.count();
    }

    /**
     * 根据ID查询人员列表
     * @param context
     * @return
     */
    public static List<Person> getPersonListById(Context context,long id) {
        checkDaoSession(context);
        QueryBuilder<Person> queryBuilder = daoSession.queryBuilder(Person.class);
        return queryBuilder.where(PersonDao.Properties.Id.eq(id)).list();
    }

    /**
     * 获取人脸库
     * @return
     */
    public static List<Person> getPersonList(Context context){
        checkDaoSession(context);
        QueryBuilder<Person> queryBuilder = daoSession.queryBuilder(Person.class);
        return queryBuilder.list();
    }

    /**
     * 添加一个人员信息到数据库
     * @param context
     * @param person
     */
    public static void addPersonToDB(Context context,Person person) {
        checkDaoSession(context);
        daoSession.insertOrReplace(person);
    }

    /**
     * 根据id删除人员信息
     * @param context
     * @param id
     */
    public static void deletePersonById(Context context,long id) {
        checkDaoSession(context);
        daoSession.queryBuilder(Person.class).where(PersonDao.Properties.Id.eq(id))
                .buildDelete().executeDeleteWithoutDetachingEntities();
    }

    /**
     * 根据卡号查询人员信息
     * @return
     */
    public static List<Person> getPersonByNumber(Context context,String number){
        checkDaoSession(context);
        QueryBuilder<Person> queryBuilder = daoSession.queryBuilder(Person.class);
        return queryBuilder.where(PersonDao.Properties.Number.eq(number)).list();
    }

    /**
     * 添加记录
     * @param context
     * @param record
     */
    public static void addRecord(Context context,Record record){
        checkDaoSession(context);
        daoSession.insertOrReplace(record);
    }

    /**
     * 根据时间戳删除记录
     * @param context
     * @param timestamp
     */
    public static void deleteRecordByTimestamp(Context context,long timestamp){
        checkDaoSession(context);
        daoSession.queryBuilder(Record.class).where(RecordDao.Properties.Timestamp.eq(timestamp))
                .buildDelete().executeDeleteWithoutDetachingEntities();
    }

    /**
     * 获取记录
     * @param context
     * @return
     */
    public static List<Record> getRecordList(Context context){
        return daoSession.queryBuilder(Record.class).list();
    }

}
