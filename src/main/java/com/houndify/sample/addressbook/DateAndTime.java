package com.houndify.sample.addressbook;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.hound.java.sanity.SanityCheck;
import com.hound.java.sanity.SanityCheckable;
import com.hound.java.sanity.SanityException;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Modeled after the confluence docs below.
 * <p>
 * https://docs.soundhound.com:40443/display/vandroid/DateAndTime
 */
@SanityCheck
@JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
public class DateAndTime implements SanityCheckable {

    @JsonProperty("TimeZone")
    String timeZone;
    @JsonProperty("Date")
    Date date;
    @JsonProperty("Time")
    Time time;

    public DateAndTime() {
        // Nothing
    }

    public DateAndTime(final Calendar cal) {
        timeZone = cal.getTimeZone().getID();
        setDate(new Date(cal));
        setTime(new Time(cal));
    }

    /**
     * This can return null if the server does not send back a time zone.
     *
     * @return
     */
    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(final String timeZone) {
        this.timeZone = timeZone;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(final Date date) {
        this.date = date;
    }

    public Time getTime() {
        return time;
    }

    public void setTime(final Time time) {
        this.time = time;
    }

    // Helper Methods

    /**
     * Returns true if the exact date is not known and is symbolic.
     *
     * @return
     */
    public boolean isDateAmbiguous() {
        return date.isSymbolic();
    }

    /**
     * Returns true if the exact time is not known and is symbolic.
     *
     * @return
     */
    public boolean isTimeAmbiguous() {
        return time.isSymbolic();
    }

    /**
     * Returns true if the exact date, time, and time zone are all known.
     * <p>
     * Useful to know if you are dealing with an exact time stamp.
     *
     * @return
     */
    public boolean isAbsolute() {
        return timeZone != null && !isDateAmbiguous() && !isTimeAmbiguous();
    }

    /**
     * Returns a {@link TimeZone} object for convenience running it through
     * {@link TimeZone#getTimeZone(String)}. If @ #getTimeZone()} returns null,
     * the default time zone will be return via {@link TimeZone#getDefault()}.
     * <p>
     * <b>Note</b> that if the desire is to print the timezone, that information should instead
     * be obtained via {@link #getCalendar()}, as the returned TimeZone object here has no
     * context and therefore no knowledge of whether it is in DST or not.
     *
     * @return
     */
    public TimeZone getTimeZoneObj() {
        return timeZone != null ? TimeZone.getTimeZone(timeZone) : TimeZone.getDefault();
    }

    public Calendar getCalendar() {
        return getCalendar(null);
    }

    public Calendar getCalendar(final TimeZone timeZone) {
        final Calendar cal = Calendar.getInstance();

        // Setup the timezone if we have one
        if (timeZone != null) {
            cal.setTimeZone(timeZone);
        } else {
            final TimeZone tmz = getTimeZoneObj();
            if (tmz != null) {
                cal.setTimeZone(tmz);
            }
        }

        // Date
        getDate().populateCalendar(cal);

        // Time
        getTime().populateCalendar(cal);

        return cal;
    }

    /**
     * Returns the timestamp of this object in UTC.
     *
     * @return
     */
    public long getTimestamp() {
        return getCalendar().getTimeInMillis();
    }

    @Override
    public String toString() {
        final StringBuilder str = new StringBuilder();
        str.append("DateAndTime[");

        if (date != null) {
            str.append(date);
        } else {
            str.append("unknown date");
        }

        str.append(", ");

        if (time != null) {
            str.append(time);
        } else {
            str.append("unknown time");
        }

        str.append(", ");

        if (timeZone != null) {
            str.append(timeZone);
        } else {
            str.append("unknown timezone");
        }

        str.append(", ");

        str.append(getTimestamp());
        str.append("]");

        return str.toString();
    }

    @Override
    public void sanityCheck() throws SanityException {
        if (time.isSymbolic() && time.hour >= 0) {
            throw new SanityException("Time object cannot have a symbolic value AND a precise time");
        }
        if (date.isSymbolic() && date.dayOfMonth >= 0) {
            throw new SanityException("Date object cannot have a symbolic value AND a precise date");
        }
    }


    @JsonAutoDetect(
            fieldVisibility = JsonAutoDetect.Visibility.NONE,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE,
            getterVisibility = JsonAutoDetect.Visibility.NONE,
            setterVisibility = JsonAutoDetect.Visibility.NONE)
    public static class Date {

        // These values match the values parsed from the JSON
        //private static final String START_OF_YEAR = "start-of-year";

        public enum Symbolic {
            // Default values for the symbolics
            UNKNOWN(0, 1, 2014);

            private final int dayOfMonth;
            private final int month;
            private final int year;

            private Symbolic(final int m, final int md, final int y) {
                month = m;
                dayOfMonth = md;
                year = y;
            }

            public void setCalendarFields(final Calendar cal) {
                if (this != UNKNOWN) {
                    cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    cal.set(Calendar.MONTH, month);
                    cal.set(Calendar.YEAR, year);
                }
            }
        }

        @JsonProperty("DayOfMonth")
        int dayOfMonth = -1;
        @JsonProperty("Month")
        int month = -1;
        @JsonProperty("Year")
        int year = -1;

        @JsonProperty("Symbolic")
        String symbolic;

        public Date() {
            // Nothing
        }

        public Date(final Calendar cal) {
            dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
            month = cal.get(Calendar.MONTH) + 1;
            year = cal.get(Calendar.YEAR);
        }

        public int getDayOfMonth() {
            return dayOfMonth;
        }

        public void setDayOfMonth(final int dayOfMonth) {
            this.dayOfMonth = dayOfMonth;
        }

        public int getMonth() {
            return month;
        }

        public void setMonth(final int month) {
            this.month = month;
        }

        public int getYear() {
            return year;
        }

        public void setYear(final int year) {
            this.year = year;
        }

        public String getSymbolic() {
            return symbolic;
        }

        public void setSymbolic(final String symbolic) {
            this.symbolic = symbolic;
        }

        public boolean isSymbolic() {
            return symbolic != null;
        }

        public Symbolic getSymbolicEnum() {
            if (!isSymbolic()) {
                throw new IllegalStateException("This date object is not symbolic");
            }

            return Symbolic.UNKNOWN;
        }

        public void populateCalendar(final Calendar calendar) {
            if (isSymbolic()) {
                getSymbolicEnum().setCalendarFields(calendar);
            } else {
                calendar.set(getYear(), getMonth() - 1, getDayOfMonth());
            }
        }

        @Override
        public String toString() {
            return symbolic != null ? symbolic.toString() : month + "/" + dayOfMonth + "/" + year;
        }
    }


    @JsonAutoDetect(
            fieldVisibility = JsonAutoDetect.Visibility.NONE,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE,
            getterVisibility = JsonAutoDetect.Visibility.NONE,
            setterVisibility = JsonAutoDetect.Visibility.NONE)
    public static class Time {

        // These values match the values parsed from the JSON
        private static final String START_OF_DAY = "start-of-day";
        private static final String END_OF_DAY = "end-of-day";

        public enum Symbolic {
            // Default values for the symbolics
            START_OF_DAY(0, 0, 0),
            END_OF_DAY(23, 59, 59),
            UNKNOWN(0, 0, 0);

            private final int defaultHour;
            private final int defaultMinute;
            private final int defaultSecond;

            private Symbolic(final int hour, final int minute, final int second) {
                defaultHour = hour;
                defaultMinute = minute;
                defaultSecond = second;
            }

            public void setCalendarFields(final Calendar cal) {
                cal.set(Calendar.HOUR_OF_DAY, defaultHour);
                cal.set(Calendar.MINUTE, defaultMinute);
                cal.set(Calendar.SECOND, defaultSecond);
                cal.set(Calendar.MILLISECOND, 0); // 0 for now
            }
        }

        @JsonProperty("Second")
        int second = -1;
        @JsonProperty("Minute")
        int minute = -1;
        @JsonProperty("Hour")
        int hour = -1;
        @JsonProperty("AmPmUnknown")
        boolean amPmUnknown;

        @JsonProperty("Symbolic")
        String symbolic;

        public Time() {
            // Nothing
        }

        public Time(final Calendar cal) {
            second = cal.get(Calendar.SECOND);
            minute = cal.get(Calendar.MINUTE);
            hour = cal.get(Calendar.HOUR_OF_DAY);
            amPmUnknown = false;
        }

        public int getSecond() {
            return second;
        }

        public void setSecond(final int second) {
            this.second = second;
        }

        public int getMinute() {
            return minute;
        }

        public void setMinute(final int minute) {
            this.minute = minute;
        }

        public int getHour() {
            return hour;
        }

        public void setHour(final int hour) {
            this.hour = hour;
        }

        public boolean isAmPmUnknown() {
            return amPmUnknown;
        }

        public void setAmPmUnknown(final boolean amPmUnknown) {
            this.amPmUnknown = amPmUnknown;
        }

        public String getSymbolic() {
            return symbolic;
        }

        public void setSymbolic(final String symbolic) {
            this.symbolic = symbolic;
        }

        // Helpers

        @Override
        public String toString() {
            if (symbolic == null) {
                return hour + ":" + minute + ":" + second;
            } else {
                return symbolic;
            }
        }

        public boolean isSymbolic() {
            return symbolic != null;
        }

        public Symbolic getSymbolicEnum() {
            if (!isSymbolic()) {
                throw new IllegalStateException("This time object is not symbolic");
            }

            if (symbolic.equals(START_OF_DAY)) {
                return Symbolic.START_OF_DAY;
            } else if (symbolic.equals(END_OF_DAY)) {
                return Symbolic.END_OF_DAY;
            } else {
                return Symbolic.UNKNOWN;
            }
        }

        public void populateCalendar(final Calendar cal) {
            if (isSymbolic()) {
                getSymbolicEnum().setCalendarFields(cal);
            } else {
                cal.set(Calendar.HOUR_OF_DAY, getHour());
                cal.set(Calendar.MINUTE, getMinute());
                cal.set(Calendar.SECOND, getSecond());
                cal.set(Calendar.MILLISECOND, 0); // 0 for now
            }
        }
    }
}