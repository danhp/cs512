// -------------------------------// Adapted from Kevin T. Manley// CSE 593// -------------------------------package middleware;import java.io.Serializable;import java.util.*;// A specialization of Hashtable with some extra diagnostics.public class RMHashtable extends Hashtable {    RMHashtable() {      super();    }    public String toString() {        String s = "RMHashtable { \n";        Object key = null;        for (Enumeration e = keys(); e.hasMoreElements();) {            key = e.nextElement();            String value = (String) get(key);            s = s + "  [key = " + key + "] " + value + "\n";        }        s = s + "}";        return s;    }    public void dump() {        System.out.println(toString());    }}