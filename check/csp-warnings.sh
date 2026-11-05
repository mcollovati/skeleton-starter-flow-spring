#!/bin/sh
# Prints, per CSP warning: the API frame and the first app/Flow caller frames
awk '/CspViolationException:/{if(s)print s; s="*"; c=0; next}
/^[ \t]+at /{ if(s){ c++; f=$2; sub(/\(.*/,"",f); if(c<=2 || f ~ /org\.vaadin\.example|ensureResizeListener|flashClass|AuthenticationContext\.(logout|requestRoundTrip)|pushHistoryState/) { if(index(s,f)==0) s=s" <- "f } if(f ~ /org\.vaadin\.example/){print s; s=""} } next}
END{if(s)print s}' "$1" | sort | uniq -c
