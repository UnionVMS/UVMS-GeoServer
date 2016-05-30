/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor.web;

import java.util.Calendar;
import java.util.Date;

import org.geoserver.monitor.Monitor;
import org.jfree.data.time.Hour;
import org.jfree.data.time.Minute;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.Second;


public class DailyActivityPanel extends ActivityChartBasePanel {

    public DailyActivityPanel(String id, Monitor monitor) {
        super(id, monitor);
    }

    @Override
    protected Date[] getDateRange() {
        Date now = Calendar.getInstance().getTime();
        
        Calendar then = Calendar.getInstance();
        then.setTime(now);
        then.set(Calendar.HOUR_OF_DAY, 0);
        then.set(Calendar.MINUTE, 0);
        then.set(Calendar.SECOND, 0);
        
        return new Date[]{then.getTime(), now};
    };
    
    @Override
    protected RegularTimePeriod getTimePeriod(Date time) {
        return new Second(time);
        //return new Minute(time);
        //return new Hour(time);
    }
    
    @Override
    protected String getChartTitle(Date[] range) {
        return "Activity " + FORMAT.format(range[0]);
    }
}
