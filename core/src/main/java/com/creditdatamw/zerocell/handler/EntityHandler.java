package com.creditdatamw.zerocell.handler;

import com.creditdatamw.zerocell.ReaderUtil;
import com.creditdatamw.zerocell.ZeroCellException;
import com.creditdatamw.zerocell.ZeroCellReader;
import com.creditdatamw.zerocell.annotation.Column;
import com.creditdatamw.zerocell.annotation.RowNumber;
import com.creditdatamw.zerocell.column.ColumnInfo;
import com.creditdatamw.zerocell.column.ColumnMapping;
import com.creditdatamw.zerocell.converter.Converter;
import com.creditdatamw.zerocell.converter.Converters;
import com.creditdatamw.zerocell.converter.NoopConverter;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static com.creditdatamw.zerocell.column.ColumnMapping.parseColumnMappingFromAnnotations;

public class EntityHandler<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(EntityHandler.class);

    private static final String DEFAULT_SHEET = "uploads";

    private final Class<T> type;
    private final EntityExcelSheetHandler<T> entitySheetHandler;
    private final String sheetName;
    private final boolean skipHeaderRow;
    private final int skipFirstNRows;
    private final int maxRowNumber;

    public EntityHandler(Class<T> clazz, boolean skipHeaderRow, int skipFirstNRows, int maxRowNumber) {
        Objects.requireNonNull(clazz);
        assert (skipFirstNRows >= 0);
        this.type = clazz;
        this.sheetName = DEFAULT_SHEET;
        this.entitySheetHandler = createSheetHandler(clazz, null);
        this.skipHeaderRow = skipHeaderRow;
        this.skipFirstNRows = skipFirstNRows;
        this.maxRowNumber = maxRowNumber;
    }

    public EntityHandler(Class<T> clazz, String sheetName, boolean skipHeaderRow, int skipFirstNRows, int maxRowNumber) {
        Objects.requireNonNull(clazz);
        assert (skipFirstNRows >= 0);
        this.type = clazz;
        this.sheetName = sheetName;
        this.entitySheetHandler = createSheetHandler(clazz, null);
        this.skipHeaderRow = skipHeaderRow;
        this.skipFirstNRows = skipFirstNRows;
        this.maxRowNumber = maxRowNumber;
    }

    public EntityHandler(Class<T> clazz, ColumnMapping columnMapping, boolean skipHeaderRow, int skipFirstNRows, int maxRowNumber) {
        Objects.requireNonNull(clazz);
        assert (skipFirstNRows >= 0);
        this.type = clazz;
        this.sheetName = DEFAULT_SHEET;
        this.entitySheetHandler = createSheetHandler(clazz, columnMapping);
        this.skipHeaderRow = skipHeaderRow;
        this.skipFirstNRows = skipFirstNRows;
        this.maxRowNumber = maxRowNumber;
    }

    public EntityHandler(Class<T> clazz, String sheetName, ColumnMapping columnMapping, boolean skipHeaderRow, int skipFirstNRows, int maxRowNumber) {
        Objects.requireNonNull(clazz);
        assert (skipFirstNRows >= 0);
        this.type = clazz;
        this.sheetName = sheetName;
        this.entitySheetHandler = createSheetHandler(clazz, columnMapping);
        this.skipHeaderRow = skipHeaderRow;
        this.skipFirstNRows = skipFirstNRows;
        this.maxRowNumber = maxRowNumber;
    }

    private boolean isRowNumberValueSetted() {
        return this.maxRowNumber != 0 && this.maxRowNumber != this.skipFirstNRows;
    }

    public String getSheetName() {
        return sheetName;
    }

    public EntityExcelSheetHandler<T> getEntitySheetHandler() {
        return entitySheetHandler;
    }

    public boolean isSkipHeaderRow() {
        return skipHeaderRow;
    }

    public int getSkipFirstNRows() {
        return skipFirstNRows;
    }

    public int getMaxRowNumber() {
        return maxRowNumber;
    }

    @SuppressWarnings("unchecked")
    private EntityExcelSheetHandler<T> createSheetHandler(Class<T> clazz, ColumnMapping columnMapping) {
        if (columnMapping == null) {
            columnMapping = parseColumnMappingFromAnnotations(clazz);
        }
        final ColumnInfo rowNumberColumn = columnMapping.getRowNumberInfo();

        final List<ColumnInfo> columnInfos = columnMapping.getColumns();
        Map<Integer, ColumnInfo> infoMap = initColumnInfosMap(columnInfos);
        return new EntityExcelSheetHandler(rowNumberColumn, infoMap);
    }

    /**
     * Create th map, where key is the index from @Column annotation,
     * and value is the ColumnInfo.
     * The method also checks the indexes duplicates and throws a ZeroCellException in this case
     */
    private Map<Integer, ColumnInfo> initColumnInfosMap(List<ColumnInfo> columnInfos) throws ZeroCellException {
        Map<Integer, ColumnInfo> map = new HashMap<>();
        columnInfos.forEach(info -> {
            int index = info.getIndex();
            if (Objects.isNull(map.get(index))) {
                map.put(index, info);
            } else {
                throw new ZeroCellException("Cannot map two columns to the same index: " + index);
            }
        });
        return map;
    }

    /**
     * Returns the extracted entities as an immutable list.
     *
     * @return an immutable list of the extracted entities
     */
    public List<T> readAsList() {
        List<T> list = Collections.unmodifiableList(this.entitySheetHandler.read(null, sheetName));
        return list;
    }

    public void process(File file) throws ZeroCellException {
        ReaderUtil.process(file, sheetName, this.entitySheetHandler);
    }

    public Class<T> getEntityClass() {
        return type;
    }
}