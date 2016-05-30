/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test.onlineTest.setup;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;

import org.geoserver.test.onlineTest.support.AbstractReferenceDataSetup;
import org.geoserver.test.onlineTest.support.DatabaseUtil;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.geotools.jdbc.JDBCDataStoreFactory;

/**
 * Postgis data setup for the data reference set online test
 * 
 * @author Victor Tey, CSIRO Earth Science and Resource Engineering
 */
public class ReferenceDataPostgisSetup extends AbstractReferenceDataSetup {
    private final String versiontbl = "data_version";

    private InputStream script;

    private final double scriptVersion = 1.0;

    public ReferenceDataPostgisSetup() throws Exception {
        this.script = this.getClass().getResourceAsStream("/RefDataSet/Postgis_Data_ref_set.sql");
    }

    /**
     * Returns PostgisNGDataStoreFactory
     */
    @Override
    public JDBCDataStoreFactory createDataStoreFactory() {
        return new PostgisNGDataStoreFactory();
    }

    protected void runSqlInsertScript() throws Exception {
        DatabaseUtil du = new DatabaseUtil();
        ArrayList<String> sqls = du.splitPostgisSQLScript(script);
        // run the script as a single shot, going back and forth line
        // by line takes forever to run (more than a minute)
        String pgScript = rebuildAsSingle(sqls);
        this.run(pgScript);
        this.setDataVersion(this.scriptVersion);

    }

    private String rebuildAsSingle(ArrayList<String> sqls) {
        StringBuilder sb = new StringBuilder();
        for (String sql : sqls) {
            sb.append(sql).append("\n");
        }
        
        return sb.toString();
    }

    // these private helper class might be useful in the future. feel free to change its access
    // modifier
    private void setDataVersion(double version) throws Exception {
        this.run("DROP TABLE IF EXISTS public." + versiontbl);
        this.run("CREATE TABLE public." + versiontbl + " ("
                + "name character varying(100) NOT NULL, " + "version double precision,"
                + "insert_date timestamp without time zone);");
        this.run("insert into public." + versiontbl
                + "(name,version,insert_date) values('Data reference set'," + version
                + ",current_timestamp)");

    }

    @Override
    public String getDatabaseID() {
        return "postgis";
    }

    @Override
    public void setUp() throws Exception {
        runSqlInsertScript();
    }

    @Override
    protected Properties createExampleFixture() {
        Properties fixture = new Properties();
        fixture.put("password", "MyPassword");
        fixture.put("passwd", "MyPassword");
        fixture.put("user", "user");
        fixture.put("port", "5432");
        fixture.put("url", "jdbc:postgresql://MyHost/MyDatabase");
        fixture.put("host", "MyHost");
        fixture.put("database", "MyDatabase");
        fixture.put("driver", "org.postgresql.Driver");
        fixture.put("dbtype", "postgisng");
        return fixture;
    }

}
