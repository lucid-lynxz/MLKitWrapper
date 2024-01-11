package org.lynxz.mlkit.base.util;


public class FakeGuavaUtil {
    private static final String TAG = "FakeGuavaUtil";

    /**
     * com.google.common.base.Preconditions.java
     * <p>
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * @param expression a boolean expression
     * @throws IllegalArgumentException if {@code expression} is false
     */
    public static void checkArgument(boolean expression) {
        if (!expression) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * com.google.common.base.Preconditions.java
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * @param expression   a boolean expression
     * @param errorMessage the exception message to use if the check fails; will be converted to a
     *                     string using {@link String#valueOf(Object)}
     * @throws IllegalArgumentException if {@code expression} is false
     */
    public static void checkArgument(boolean expression, Object errorMessage) {
        if (!expression) {
            throw new IllegalArgumentException(String.valueOf(errorMessage));
        }
    }

    /**
     * com.google.common.base.Preconditions.java
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * @since 20.0 (varargs overload since 2.0)
     */
    public static void checkArgument(
            boolean b, String errorMessageTemplate, int p1, int p2) {
        if (!b) {
            throw new IllegalArgumentException(lenientFormat(errorMessageTemplate, p1, p2));
        }
    }

    /**
     * com.google.common.base.Preconditions.java
     * Ensures the truth of an expression involving the state of the calling instance, but not
     * involving any parameters to the calling method.
     *
     * @param expression a boolean expression
     * @throws IllegalStateException if {@code expression} is false
     */
    public static void checkState(boolean expression) {
        if (!expression) {
            throw new IllegalStateException();
        }
    }

    /**
     * com.google.common.base.Preconditions.java
     * Ensures the truth of an expression involving the state of the calling instance, but not
     * involving any parameters to the calling method.
     *
     * @param expression   a boolean expression
     * @param errorMessage the exception message to use if the check fails; will be converted to a
     *                     string using {@link String#valueOf(Object)}
     * @throws IllegalStateException if {@code expression} is false
     */
    public static void checkState(boolean expression, Object errorMessage) {
        if (!expression) {
            throw new IllegalStateException(String.valueOf(errorMessage));
        }
    }

    /**
     * com.google.common.primitives.Ints.java
     * Returns the value nearest to {@code value} which is within the closed range {@code [min..max]}.
     *
     * <p>If {@code value} is within the range {@code [min..max]}, {@code value} is returned
     * unchanged. If {@code value} is less than {@code min}, {@code min} is returned, and if {@code
     * value} is greater than {@code max}, {@code max} is returned.
     *
     * @param value the {@code int} value to constrain
     * @param min   the lower bound (inclusive) of the range to constrain {@code value} to
     * @param max   the upper bound (inclusive) of the range to constrain {@code value} to
     * @throws IllegalArgumentException if {@code min > max}
     * @since 21.0
     */
    public static int constrainToRange(int value, int min, int max) {
        checkArgument(min <= max, "min (%s) must be less than or equal to max (%s)", min, max);
        return Math.min(Math.max(value, min), max);
    }


    /**
     * com.google.common.base.Strings.java
     * Returns the given {@code template} string with each occurrence of {@code "%s"} replaced with
     * the corresponding argument value from {@code args}; or, if the placeholder and argument counts
     * do not match, returns a best-effort form of that string. Will not throw an exception under
     * normal conditions.
     *
     * <p><b>Note:</b> For most string-formatting needs, use {@link String#format String.format},
     * {@link java.io.PrintWriter#format PrintWriter.format}, and related methods. These support the
     * full range of <a
     * href="https://docs.oracle.com/javase/9/docs/api/java/util/Formatter.html#syntax">format
     * specifiers</a>, and alert you to usage errors by throwing {@link
     * java.util.IllegalFormatException}.
     *
     * <p>In certain cases, such as outputting debugging information or constructing a message to be
     * used for another unchecked exception, an exception during string formatting would serve little
     * purpose except to supplant the real information you were trying to provide. These are the cases
     * this method is made for; it instead generates a best-effort string with all supplied argument
     * values present. This method is also useful in environments such as GWT where {@code
     * String.format} is not available. As an example, method implementations of the  @link
     * Preconditions  class use this formatter, for both of the reasons just discussed.
     *
     * <p><b>Warning:</b> Only the exact two-character placeholder sequence {@code "%s"} is
     * recognized.
     *
     * @param template a string containing zero or more {@code "%s"} placeholder sequences. {@code
     *                 null} is treated as the four-character string {@code "null"}.
     * @param args     the arguments to be substituted into the message template. The first argument
     *                 specified is substituted for the first occurrence of {@code "%s"} in the template, and so
     *                 forth. A {@code null} argument is converted to the four-character string {@code "null"};
     *                 non-null values are converted to strings using {@link Object#toString()}.
     * @since 25.1
     */
    public static String lenientFormat(String template, Object... args) {
        template = String.valueOf(template); // null -> "null"

        if (args == null) {
            args = new Object[]{"(Object[])null"};
        } else {
            for (int i = 0; i < args.length; i++) {
                args[i] = lenientToString(args[i]);
            }
        }

        // start substituting the arguments into the '%s' placeholders
        StringBuilder builder = new StringBuilder(template.length() + 16 * args.length);
        int templateStart = 0;
        int i = 0;
        while (i < args.length) {
            int placeholderStart = template.indexOf("%s", templateStart);
            if (placeholderStart == -1) {
                break;
            }
            builder.append(template, templateStart, placeholderStart);
            builder.append(args[i++]);
            templateStart = placeholderStart + 2;
        }
        builder.append(template, templateStart, template.length());

        // if we run out of placeholders, append the extra args in square braces
        if (i < args.length) {
            builder.append(" [");
            builder.append(args[i++]);
            while (i < args.length) {
                builder.append(", ");
                builder.append(args[i++]);
            }
            builder.append(']');
        }

        return builder.toString();
    }

    /**
     * com.google.common.base.Strings.java
     */
    private static String lenientToString(Object o) {
        try {
            return String.valueOf(o);
        } catch (Exception e) {
            // Default toString() behavior - see Object.toString()
            String objectToString =
                    o.getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(o));
            LogWrapper.e(TAG, "lenientToString: ", e);
            // Logger is created inline with fixed name to avoid forcing Proguard to create another class.
//            Logger.getLogger("com.google.common.base.Strings")
//                    .log(WARNING, "Exception during lenientFormat for " + objectToString, e);
            return "<" + objectToString + " threw " + e.getClass().getName() + ">";
        }
    }
}
