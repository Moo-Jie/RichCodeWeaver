package com.rich.codeweaver.common.utils;

import cn.hutool.core.util.StrUtil;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.net.IDN;
import java.util.Hashtable;
import java.util.Locale;

import static com.rich.codeweaver.common.constant.UserConstant.EMAIL_REGEX;

public final class EmailUtils {

    private EmailUtils() {
    }

    public static String normalizeEmail(String email) {
        if (StrUtil.isBlank(email)) {
            return "";
        }
        return StrUtil.trim(email).toLowerCase(Locale.ROOT);
    }

    public static boolean isValidEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (StrUtil.isBlank(normalizedEmail) || !normalizedEmail.matches(EMAIL_REGEX)) {
            return false;
        }
        int atIndex = normalizedEmail.indexOf('@');
        if (atIndex <= 0 || atIndex != normalizedEmail.lastIndexOf('@')) {
            return false;
        }
        String localPart = normalizedEmail.substring(0, atIndex);
        String domain = normalizedEmail.substring(atIndex + 1);
        if (!isValidLocalPart(localPart) || !isValidDomain(domain)) {
            return false;
        }
        return hasDnsRecord(domain, "MX") || hasDnsRecord(domain, "A");
    }

    private static boolean isValidLocalPart(String localPart) {
        if (StrUtil.isBlank(localPart) || localPart.length() > 64) {
            return false;
        }
        return !localPart.startsWith(".")
                && !localPart.endsWith(".")
                && !localPart.contains("..");
    }

    private static boolean isValidDomain(String domain) {
        if (StrUtil.isBlank(domain) || domain.length() > 253 || domain.contains("..")) {
            return false;
        }
        String[] labels = domain.split("\\.");
        if (labels.length < 2) {
            return false;
        }
        for (String label : labels) {
            if (StrUtil.isBlank(label) || label.length() > 63) {
                return false;
            }
            if (label.startsWith("-") || label.endsWith("-")) {
                return false;
            }
        }
        String topLevelDomain = labels[labels.length - 1];
        return topLevelDomain.matches("[a-z]{2,24}");
    }

    private static boolean hasDnsRecord(String domain, String recordType) {
        String asciiDomain;
        try {
            asciiDomain = IDN.toASCII(domain);
        } catch (IllegalArgumentException e) {
            return false;
        }
        Hashtable<String, String> environment = new Hashtable<>();
        environment.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
        DirContext dirContext = null;
        try {
            dirContext = new InitialDirContext(environment);
            Attributes attributes = dirContext.getAttributes(asciiDomain, new String[]{recordType});
            if (attributes == null) {
                return false;
            }
            Attribute attribute = attributes.get(recordType);
            if (attribute == null || attribute.size() == 0) {
                return false;
            }
            NamingEnumeration<?> values = attribute.getAll();
            while (values.hasMore()) {
                Object value = values.next();
                if (value != null && StrUtil.isNotBlank(value.toString())) {
                    return true;
                }
            }
            return false;
        } catch (NamingException e) {
            return false;
        } finally {
            if (dirContext != null) {
                try {
                    dirContext.close();
                } catch (NamingException ignored) {
                }
            }
        }
    }
}
