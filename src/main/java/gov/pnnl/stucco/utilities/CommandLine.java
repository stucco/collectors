package gov.pnnl.stucco.utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * A utility for doing most of the bookkeeping work needed to parse
 * command-line arguments.  It assumes that any standalone values in the
 * command line will precede any switch keys and their switch values.
 *
 * <p> Example: In the command line "PROGRAM myfile -fast -name foo -range 1 5"
 * <ul>
 * <li>"myfile" is a standalone value
 * <li>"-fast" is a switch key with no switch values,
 * <li>"-name" is a switch key with 1 switch value, "foo"
 * <li>"-range" is a switch key with 2 switch values, "1" and "5"
 * </ul>
 *
 * <p> Example:
 * <pre>
 * {@code
 *   public static void main(String[] args) {
 *     try {
 *       CommandLine parser = new CommandLine();
 *       parser.add0("-O");
 *       parser.add1("-n");
 *       parser.parse(args);
 *
 *       String[] standalone = parser.getStandaloneValues();
 *       String filename = null;
 *       if (standalone.length > 0) {
 *         filename = standalone[0];
 *       }
 *
 *       boolean flag = parser.found("-O");
 *
 *       int n = 0;
 *       if (parser.found("-n")) {
 *         n = parser.getValue();
 *       }
 *     }
 *     catch (CommandLine.UsageException ex) {
 *       System.err.println("Usage: myprogram filename -O -n 2");
 *       System.exit(1);
 *     }
 *
 *     // ...
 *   }
 * }
 * </pre>
 *
 * <p> UnsupportedOperationExceptions are throw when the reads don't match the setup.
 *
 *
 * @author Grant Nakamura, February 2013
 *
 */
public final class CommandLine {

  /** Key for Switch representing standalone values. */
  public static final String STANDALONE = "";

  /** Exception indicating an invalid command line has been detected.*/
  @SuppressWarnings("serial")
  public static class UsageException extends Exception {
    public UsageException() {
      super();
    }

    public UsageException(String msg) {
      super(msg);
    }
  }

  /** Data struct for a switch. */
  private class Switch {
    /** The key for this switch. */
    String key;

    /** Minimum number of values for this switch. */
    int min;

    /** Maximum number of values for this switch. */
    int max;

    /** Whether this switch is required to be found. */
    boolean required;

    /** Whether this switch was found on the command line. */
    boolean found;

    /** Values found for this switch. */
    List<String> valueList = new ArrayList<String>();

    /** Creates a data record to be associated with a switch. */
    public Switch(String key, int min, int max) {
      if (min < 0 || max < min) {
        throw new IndexOutOfBoundsException("Invalid range for number of values: " + min + ", " + max);
      }

      this.key = key;
      this.min = min;
      this.max = max;
    }

    /** Adds a value for this switch. */
    public void add(String value) {
      valueList.add(value);
    }
  }

  /** Map of switch key to the data record for it. */
  private Map<String, Switch> switchMap = new HashMap<String, Switch>();

  /** The current key implicitly used for keyless convenience methods. */
  private String currentKey;


  /** Constructs a command-line parser. */
  public CommandLine() {
    // We always have a standalone entry, and it always counts as found (possibly with zero values)
    setAllowedStandalone(0, Integer.MAX_VALUE);

    currentKey = STANDALONE;
  }


  // SET UP


  // Adding switches

  /**
   * Adds a switch key with a range for the number of values expected.
   *
   * @param min  Minimum number of values for this key
   * @param max  Maximum number of values for this key
   */
  public void add(String key, int min, int max) {
    currentKey = key;
    switchMap.put(key, new Switch(key, min, max));
  }

  /** Adds a switch with n values expected. */
  public void add(String key, int n) {
    add(key, n, n);
  }

  /** Adds a switch with 0 values expected. */
  public void add0(String key) {
    add(key, 0);
  }

  /** Adds a switch with 1 value expected. */
  public void add1(String key) {
    add(key, 1);
  }

  /** Adds multiple switches each with 1 value expected. */
  public void add1(Iterable<String> keys) {
    for (String key : keys) {
      add1(key);
    }
  }

  /** Adds multiple switches each with 1 value expected. */
  public void add1(String[] keys) {
    add1(Arrays.asList(keys));
  }


  // Making switches required

  /** Makes a switch required. By default, switches are optional. */
  public void require(String key) {
    Switch data = getData(key);
    data.required = true;
  }

  /** Makes the current switch required. By default, switches are optional. */
  public void require() {
    require(currentKey);
  }

  /** Makes multiple switches required. By default, switches are optional.  */
  public void require(Iterable<String> keys) {
    for (String key : keys) {
      require(key);
    }
  }

  /** Makes multiple switches required. By default, switches are optional. */
  public void require(String[] keys) {
    require(Arrays.asList(keys));
  }


  // Standalone values

  /** Sets the allowed number of standalone values. */
  public void setAllowedStandalone(int min, int max) {
    add(STANDALONE, min, max);

    // Standalone data always counts as found (possibly with zero values)
    Switch data = getData(STANDALONE);
    data.found = true;
  }



  // PARSE

  /** Parses command-line args for the switches that were added. */
  public void parse(String[] args) throws UsageException {
    // Until we see a switch, any values are standalone
    Switch data = getData(STANDALONE);

    // For each token
    for (int i = 0; i < args.length; i++) {
      String token = args[i];

      if (isKey(token)) {
        // Found a switch; make it current
        data = getData(token);
        data.found = true;
      }
      else {
        // Found a value; add it to the current switch
        data.add(token);
      }
    }

    // Check expectations
    validate();
  }


  // READ

  /** Gets whether a given key appears in the command line. */
  public boolean found(String key) {
    Switch data = getData(key);
    return data.found;
  }

  /**
   * Gets the single value for a key.
   *
   * @return Switch value (null => the key wasn't in the command line)
   *
   * @throws UnsupportedOperationException if the key doesn't take exactly one value
   */
  public String getValue(String key) {
    Switch data = getData(key);
    if (data.min != 1  ||  data.max != 1) {
      throw new UnsupportedOperationException("Switch '" + key + "' wasn't declared to take exactly one value");
    }

    return (data.found?  data.valueList.get(0) : null);
  }

  /**
   * Gets the single value for the current key.
   *
   * @return Switch value (null => the key wasn't in the command line)
   *
   * @throws UnsupportedOperationException if the key doesn't take exactly one value
   */
  public String getValue() {
    String value = getValue(currentKey);
    return value;
  }

  /**
   * Gets the single integer value for a key.
   *
   * @return Switch value (null => the key wasn't in the command line)
   */
  public Integer getInt(String key) throws UsageException {
    String value = getValue(key);
    try {
      return (value == null?  null : Integer.parseInt(value));
    }
    catch (NumberFormatException e) {
      throw new UsageException("Value '" + value + "' is not an integer");
    }
  }

  /**
   * Gets the single integer value for the current key.
   *
   * @return Switch value (null => the key wasn't in the command line)
   *
   * @throws CommandLine.UsageException if value isn't an int (or null)
   */
  public Integer getInt() throws UsageException {
    Integer n = getInt(currentKey);
    return n;
  }

  /**
   * Gets the single double value for a key.
   *
   * @return Switch value (null => the key wasn't in the command line)
   *
   * @throws CommandLine.UsageException if the value isn't a double
   */
  public Double getDouble(String key) throws UsageException {
    String value = getValue(key);
    try {
      return (value == null?  null : Double.parseDouble(value));
    }
    catch (NumberFormatException e) {
      throw new UsageException("Value '" + value + "' is not a double");
    }
  }


  /**
   * Gets the single double value for the current key.
   *
   * @return Switch value (null => the key wasn't in the command line)
   *
   * @throws CommandLine.UsageException if the value isn't a double
   */
  public Double getDouble() throws UsageException {
    Double x = getDouble(currentKey);
    return x;
  }

  /**
   * Gets the values found for a key.
   *
   * @return List of values (null => key not found in command line)
   */
  public List<String> getValueList(String key) {
    Switch data = getData(key);
    return (data.found?  data.valueList : null);
  }

  /**
   * Gets the values for a key as an array.
   *
   * @return Array of values (null => the key wasn't found)
   */
  public String[] getValues(String key) {
    Switch data = getData(key);
    return (data.found?  data.valueList.toArray(new String[0]) : null);
  }

  /**
   * Gets the number of values for a key.
   *
   * @return Number of values (-1 => key wasn't in the command line)
   */
  public int getValueCount(String key) {
    Switch data = getData(key);
    return (data.found?  data.valueList.size() : -1);
  }

  /** Gets the standalone values. */
  public List<String> getStandaloneValueList() {
    List<String> valueList = getValueList(STANDALONE);
    return valueList;
  }

  /** Gets the standalone values as an array. */
  public String[] getStandaloneValues() {
    String[] values = getValues(STANDALONE);
    return values;
  }

  /** Gets the number of standalone values found. */
  public int getStandaloneValueCount() {
    int count = getValueCount(STANDALONE);
    return count;
  }


  // Utility methods

  /** Gets whether a key was added. */
  private boolean isKey(String key) {
    return switchMap.containsKey(key);
  }

  /** Gets the data for a key, assuming that the key was added. */
  private Switch getData(String key) {
    if (!isKey(key)) {
      throw new UnsupportedOperationException("Switch '" + key + "' wasn't added");
    }

    currentKey = key;
    Switch data = switchMap.get(key);
    return data;
  }

  /**
   * Checks that all required switches are found, and that the number of values
   * is correct for each switch that was found.
   */
  private void validate() throws UsageException {
    Collection<Switch> all = switchMap.values();
    for (Switch data : all) {
      if (data.found) {
        int count = data.valueList.size();
        if (count < data.min || count > data.max) {
          throw new UsageException("Switch '" + data.key + "' has an invalid number of values: " + count);
        }
      }
      else if (data.required){
        throw new UsageException("Missing required switch '" + data.key + "'");
      }
    }
  }


  // Short test

  static public void main(String[] args) {
    try {
      CommandLine parser = new CommandLine();
      parser.add1("-i");
      parser.add1("-test");
      parser.require();
      parser.parse(args);

      String is = "foo";
      if (parser.found("-i")) {
        is = parser.getValue();
      }

      double test = 3.14;
      if (parser.found("-test")) {
        test = parser.getDouble();
      }

      String[] standalone = parser.getValues(CommandLine.STANDALONE);
      for (String str : standalone) {
        System.out.println("Standalone: " + str);
      }
      System.out.println("is = " + is);
      System.out.println("test = " + test);
    }
    catch (CommandLine.UsageException ex) {
      System.err.println("Args: file1 file2 -is string -test double");
      ex.printStackTrace();
    }
  }
}

































