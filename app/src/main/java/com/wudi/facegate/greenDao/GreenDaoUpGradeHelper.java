package com.wudi.facegate.greenDao;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.github.yuweiguocn.library.greendao.MigrationHelper;

import org.greenrobot.greendao.database.Database;

/**
 * 数据库升级类
 * Created by wudi on 2020/3/4.
 */
public class GreenDaoUpGradeHelper extends DaoMaster.OpenHelper{

    public GreenDaoUpGradeHelper(Context context, String name) {
        super(context, name);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        MigrationHelper.migrate(db, new MigrationHelper.ReCreateAllTableListener() {
            @Override
            public void onCreateAllTables(Database db, boolean ifNotExists) {
                DaoMaster.createAllTables(db, ifNotExists);
            }

            @Override
            public void onDropAllTables(Database db, boolean ifExists) {
                DaoMaster.dropAllTables(db, ifExists);
            }
        }, PersonDao.class,RecordDao.class);
        // 这里可以放多个Dao.class，也就是可以做到很多table的安全升级
    }

}
