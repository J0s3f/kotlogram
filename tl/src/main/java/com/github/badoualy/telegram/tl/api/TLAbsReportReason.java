package com.github.badoualy.telegram.tl.api;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.core.TLObject;

/**
 * Abstraction level for the following constructors:
 * <ul>
 * <li>{@link TLInputReportReasonChildAbuse}: inputReportReasonChildAbuse#adf44ee3</li>
 * <li>{@link TLInputReportReasonCopyright}: inputReportReasonCopyright#9b89f93a</li>
 * <li>{@link TLInputReportReasonGeoIrrelevant}: inputReportReasonGeoIrrelevant#dbd4feed</li>
 * <li>{@link TLInputReportReasonOther}: inputReportReasonOther#e1746d0a</li>
 * <li>{@link TLInputReportReasonPornography}: inputReportReasonPornography#2e59d922</li>
 * <li>{@link TLInputReportReasonSpam}: inputReportReasonSpam#58dbcab8</li>
 * <li>{@link TLInputReportReasonViolence}: inputReportReasonViolence#1e22c78d</li>
 * </ul>
 *
 * @author Yannick Badoual yann.badoual@gmail.com
 * @see <a href="http://github.com/badoualy/kotlogram">http://github.com/badoualy/kotlogram</a>
 */
public abstract class TLAbsReportReason extends TLObject {
    public TLAbsReportReason() {
    }
}
