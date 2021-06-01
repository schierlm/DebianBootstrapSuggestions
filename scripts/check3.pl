#!/usr/bin/perl
# (c) 2021 Michael Schierl
# Licensed under MIT License

use strict;
use warnings;
use AptPkg::Cache;
use AptPkg::Source;
use POSIX qw(strftime);

my $source = AptPkg::Source->new();
my $cache = AptPkg::Cache->new();
my $packages = $cache->packages;

sub print_source_info {
	my ($f, @src) = ($_[0], @{$_[1]});
	my $size = 0;
	foreach my $file (@{$src[0]{'Files'}}) {
		$size += ${$file}{'Size'};
	}
	my $dst = "";
	foreach my $bin (sort @{$src[0]{'Binaries'}}) {
		my %pack = $packages->lookup($bin);
		next unless exists $pack{'Name'};
		$dst .= '+' if $dst ne '';
		$dst .= $pack{'Name'}.'('.$pack{'ShortDesc'}.')';
	}
	my $name = $src[0]{'Package'};
	print $f "$size\t$name\t$dst\n";
}

print "Pass 5\n";
open(my $in, "<", "tocheck2.txt") or die $!;
open(my $out1, ">", "_deps.txt") or die $!;
open(my $out2, ">", "_selfdeps.tsv") or die $!;
my $idx = 0;
my %sourcecache;

while(my $line = <$in>) {
	chomp $line;
	$idx++;
	if ($idx % 10 == 0) {
		my $tt = strftime "%H:%M:%S", localtime;
		print "\t$tt\t$idx\n";
	}
	my $selfdep = 0;
	my $depline = "$line ->";
	my %dephash = ();
	open(my $pipe, "-|", "apt-get -q -q -s --no-remove build-dep $line");
	while (<$pipe>) {
		chomp;
		next unless /^Inst /;
		s/^Inst (.*?) \(.*$/$1/;
		if (not exists $sourcecache{$_}) {
			$sourcecache{$_} = $source->find($_);
		}
		my $src = $sourcecache{$_};
		if ($src eq $line) {
			$selfdep = 1;
			last;
		}
		next if exists $dephash{$src};
		$dephash{$src} = 1;
		$depline .= " " . $src;
	}
	if ($selfdep) {
		my @src = $source->find($line, 1);
		&print_source_info($out2, \@src);
	} else {
		print $out1 "$depline\n";
	}
	close $pipe or die "Error getting dependencies of $line";
}
close $in;
close $out1;
close $out2;

print "Done";
