package io.norselibs.heimdal.integrationtest;

public class DuplicateClaimException extends Exception {
    public DuplicateClaimException(String policyNumber) {
        super("A claim already exists for policy " + policyNumber);
    }
}
