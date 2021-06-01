#!/usr/bin/perl
### (c) 2021 Michael Schierl
### Licensed under MIT License

use strict;
use warnings;
use AptPkg::Cache;
use AptPkg::Source;

my $source = AptPkg::Source->new();
my $cache = AptPkg::Cache->new();

sub print_source_info {
	my ($f, $pkg) = ($_[0], $_[1]);
	my @src = $source->find($pkg);
	my $size = 0;
	foreach my $file (@{$src[0]{'Files'}}) {
		$size += ${$file}{'Size'};
	}
	my $dst = "";
	foreach my $bin (sort @{$src[0]{'Binaries'}}) {
		my %pack = $cache->packages->lookup($bin);
		next unless exists $pack{'Name'};
		$dst .= '+' if $dst ne '';
		$dst .= $pack{'Name'}.'('.$pack{'ShortDesc'}.')';
	}
	print $f "$size\t$pkg\t$dst\n";
}

my %covered_source_packages;

print "Pass 1\n";
open(my $in, "<", "essential.list") or die $!;
open(my $out, ">", "_essential.tsv") or die $!;
while(<$in>) {
	chomp;
	s/\t*install$//;
	s/:amd64$//;
	my $src = $source->find($_);
	next if(exists $covered_source_packages{$src});
	&print_source_info($out, $src);
	$covered_source_packages{$src} = 1;
}
close $in;
close $out;

print "Pass 2\n";
open($in, "<", "build-essential.list") or die $!;
open($out, ">", "_build-essential.tsv") or die $!;
while(<$in>) {
	chomp;
	s/\t*install$//;
	s/:amd64$//;
	my $src = $source->find($_);
	next if(exists $covered_source_packages{$src});
	&print_source_info($out, $src);
	$covered_source_packages{$src} = 1;
}
close $in;
close $out;

print "Pass 3\n";
open($out, ">", "_to_check.txt") or die $!;
my $cnt = scalar keys %{$cache};
my $idx = 0;
foreach my $key (keys %{$cache}) {
	$idx++;
	if ($idx % 1000 == 0) {
		print "\t$idx / $cnt\n";
	}
	next unless exists ${$cache}{$key};
	next unless exists ${${$cache}{$key}}{'VersionList'};
	$key =~ s/:amd64$//;
	my $src = $source->find($key);
	next if(exists $covered_source_packages{$src});
	print $out "$src\n";
	$covered_source_packages{$src} = 1;
}
close $out;
print "Done";
