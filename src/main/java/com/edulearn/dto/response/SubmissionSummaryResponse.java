package com.edulearn.dto.response;

public class SubmissionSummaryResponse {
    private long draft;
    private long pending;
    private long approved;
    private long rejected;
    private long total;

    public SubmissionSummaryResponse(long draft, long pending, long approved, long rejected) {
        this.draft = draft;
        this.pending = pending;
        this.approved = approved;
        this.rejected = rejected;
        this.total = draft + pending + approved + rejected;
    }

    public long getDraft() { return draft; }
    public long getPending() { return pending; }
    public long getApproved() { return approved; }
    public long getRejected() { return rejected; }
    public long getTotal() { return total; }
}
