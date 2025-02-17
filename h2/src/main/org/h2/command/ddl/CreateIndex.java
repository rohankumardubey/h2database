/*
 * Copyright 2004-2023 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (https://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.command.ddl;

import org.h2.api.ErrorCode;
import org.h2.command.CommandInterface;
import org.h2.engine.Constants;
import org.h2.engine.Database;
import org.h2.engine.Right;
import org.h2.engine.SessionLocal;
import org.h2.index.IndexType;
import org.h2.message.DbException;
import org.h2.schema.Schema;
import org.h2.table.IndexColumn;
import org.h2.table.Table;

/**
 * This class represents the statement
 * CREATE INDEX
 */
public class CreateIndex extends SchemaCommand {

    private String tableName;
    private String indexName;
    private IndexColumn[] indexColumns;
    private int uniqueColumnCount;
    private boolean primaryKey, hash, spatial;
    private boolean ifTableExists;
    private boolean ifNotExists;
    private String comment;

    public CreateIndex(SessionLocal session, Schema schema) {
        super(session, schema);
    }

    public void setIfTableExists(boolean b) {
        this.ifTableExists = b;
    }

    public void setIfNotExists(boolean ifNotExists) {
        this.ifNotExists = ifNotExists;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public void setIndexColumns(IndexColumn[] columns) {
        this.indexColumns = columns;
    }

    @Override
    public long update() {
        Database db = getDatabase();
        boolean persistent = db.isPersistent();
        Table table = getSchema().findTableOrView(session, tableName);
        if (table == null) {
            if (ifTableExists) {
                return 0;
            }
            throw DbException.get(ErrorCode.TABLE_OR_VIEW_NOT_FOUND_1, tableName);
        }
        if (indexName != null && getSchema().findIndex(session, indexName) != null) {
            if (ifNotExists) {
                return 0;
            }
            throw DbException.get(ErrorCode.INDEX_ALREADY_EXISTS_1, indexName);
        }
        session.getUser().checkTableRight(table, Right.SCHEMA_OWNER);
        table.lock(session, Table.EXCLUSIVE_LOCK);
        if (!table.isPersistIndexes()) {
            persistent = false;
        }
        int id = getObjectId();
        if (indexName == null) {
            if (primaryKey) {
                indexName = table.getSchema().getUniqueIndexName(session,
                        table, Constants.PREFIX_PRIMARY_KEY);
            } else {
                indexName = table.getSchema().getUniqueIndexName(session,
                        table, Constants.PREFIX_INDEX);
            }
        }
        IndexType indexType;
        if (primaryKey) {
            if (table.findPrimaryKey() != null) {
                throw DbException.get(ErrorCode.SECOND_PRIMARY_KEY);
            }
            indexType = IndexType.createPrimaryKey(persistent, hash);
        } else if (uniqueColumnCount > 0) {
            indexType = IndexType.createUnique(persistent, hash);
        } else {
            indexType = IndexType.createNonUnique(persistent, hash, spatial);
        }
        IndexColumn.mapColumns(indexColumns, table);
        table.addIndex(session, indexName, id, indexColumns, uniqueColumnCount, indexType, create, comment);
        return 0;
    }

    public void setPrimaryKey(boolean b) {
        this.primaryKey = b;
    }

    public void setUniqueColumnCount(int uniqueColumnCount) {
        this.uniqueColumnCount = uniqueColumnCount;
    }

    public void setHash(boolean b) {
        this.hash = b;
    }

    public void setSpatial(boolean b) {
        this.spatial = b;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public int getType() {
        return CommandInterface.CREATE_INDEX;
    }

}
