package team.kitemc.verifymc.domain.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BatchResult {
    private final int total;
    private final int success;
    private final int failed;
    private final List<String> failedUuids;
    private final List<String> errorMessages;

    private BatchResult(Builder builder) {
        this.total = builder.total;
        this.success = builder.success;
        this.failed = builder.failed;
        this.failedUuids = Collections.unmodifiableList(new ArrayList<>(builder.failedUuids));
        this.errorMessages = Collections.unmodifiableList(new ArrayList<>(builder.errorMessages));
    }

    public int getTotal() {
        return total;
    }

    public int getSuccess() {
        return success;
    }

    public int getFailed() {
        return failed;
    }

    public List<String> getFailedUuids() {
        return failedUuids;
    }

    public List<String> getErrorMessages() {
        return errorMessages;
    }

    public boolean isAllSuccess() {
        return failed == 0;
    }

    public static BatchResult success(int count) {
        return new Builder().total(count).success(count).failed(0).build();
    }

    public static BatchResult failure(int total, List<String> failedUuids, List<String> errors) {
        return new Builder()
                .total(total)
                .success(total - failedUuids.size())
                .failed(failedUuids.size())
                .failedUuids(failedUuids)
                .errorMessages(errors)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int total;
        private int success;
        private int failed;
        private List<String> failedUuids = new ArrayList<>();
        private List<String> errorMessages = new ArrayList<>();

        public Builder total(int total) {
            this.total = total;
            return this;
        }

        public Builder success(int success) {
            this.success = success;
            return this;
        }

        public Builder failed(int failed) {
            this.failed = failed;
            return this;
        }

        public Builder failedUuids(List<String> failedUuids) {
            this.failedUuids = failedUuids != null ? failedUuids : new ArrayList<>();
            return this;
        }

        public Builder addFailedUuid(String uuid) {
            this.failedUuids.add(uuid);
            return this;
        }

        public Builder errorMessages(List<String> errorMessages) {
            this.errorMessages = errorMessages != null ? errorMessages : new ArrayList<>();
            return this;
        }

        public Builder addErrorMessage(String message) {
            this.errorMessages.add(message);
            return this;
        }

        public BatchResult build() {
            return new BatchResult(this);
        }
    }
}
