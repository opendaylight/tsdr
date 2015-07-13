package org.opendaylight.tsdr.persistence.spi;

/**
     * This class contains a list of enumerations of TSDR Data Store types.
     *
     * @author <a href="mailto:yuling_c@dell.com">YuLing Chen</a>
     *
     * Created: Feb 24, 2015
     *
     */
public enum DataStoreType {
    HBASE(0),
    H2(1),
    MySQL(2),
    SQLite(3),
    ApacheDerby(4),
    HSQLDB(5),
    CASSANDRA(6);


    int value;

    private DataStoreType(int value) {
        this.value = value;
    }

    /**
     * @return integer value
     */
    public int getIntValue() {
        return value;
    }


}
