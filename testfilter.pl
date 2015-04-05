#!/usr/bin/perl -p

use strict;

s/\b[a-z]+(\$[a-z0-9]+)*\.class/xx.class/g;
s/L[a-z]+(\$[a-z0-9]+)*;/Lxx;/g;
s/ [a-z]+(\$[a-z0-9]+)*/ xx/g if /multiple matches: /;
s/\b[a-z_]+(\$[a-z0-9]+)*(\(\S*\))/xx$1/g;

s/(matches|->) [a-zA-Z_]{1,3}\b/$1 xx/g if /field|string replace/;
s/(matches|->) [a-zA-Z_]{1,3} (\w*\(\S*\)\S+)/$1 xx $2/g if /method|class ref/;
s/-> [a-z\/M]+(\$[a-z0-9]+)*\.[a-zA-Z_<>]+ /-> xx.x /g if /(class|method|field) ref/;
s/-> [a-z\/M]+(\$[a-z0-9]+)* /-> xx /g if /class ref/;
s/-> [a-z\/M]+(\$[a-z0-9]+)* (<(cl)?init>)/-> xx $1/g if /class ref/;

s/@[[:digit:]]+/@.../g;
s/-> instruction \d+/-> instruction .../;

s/((INVOKE(VIRTUAL|STATIC|INTERFACE|SPECIAL))|((GET|PUT)(FIELD|STATIC))|NEW|CHECKCAST|INSTANCEOF|ANEWARRAY|IF_?[A-Z]+)( 0x[[:xdigit:]]{2}){2}/$1 0x.. 0x../g;
s/LDC 0x[[:xdigit:]]{2}/LDC 0x../g;
s/(LDC2?_W) 0x[[:xdigit:]]{2} 0x[[:xdigit:]]{2}/$1 0x.. 0x../g;

s/^(OS|JVM|Classpath): .*/$1: .../;
