/*  Copyright (C) 2022-2024 Daniel Dakhno

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package id.icapps.savera.database.schema;

import android.database.sqlite.SQLiteDatabase;

import id.icapps.savera.database.DBHelper;
import id.icapps.savera.database.DBUpdateScript;
import id.icapps.savera.entities.DeviceDao;

public class GadgetbridgeUpdate_42 implements DBUpdateScript {
    @Override
    public void upgradeSchema(SQLiteDatabase db) {
        if (!DBHelper.existsColumn(DeviceDao.TABLENAME, DeviceDao.Properties.ParentFolder.columnName, db)) {
            String ADD_COLUMN_CPONTAINED_FOLDER = "ALTER TABLE " + DeviceDao.TABLENAME + " ADD COLUMN "
                    + DeviceDao.Properties.ParentFolder.columnName + " TEXT";
            db.execSQL(ADD_COLUMN_CPONTAINED_FOLDER);
        }
    }

    @Override
    public void downgradeSchema(SQLiteDatabase db) {
    }
}
