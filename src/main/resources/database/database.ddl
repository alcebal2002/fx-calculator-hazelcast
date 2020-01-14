CREATE TABLE `ccy_pairs` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `ccy_pair` varchar(6) NOT NULL,
  `spread` decimal(6,5) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `ccy_pair` (`ccy_pair`)
) ENGINE=MyISAM AUTO_INCREMENT=79 DEFAULT CHARSET=latin1

CREATE TABLE historical_data (
  id int(11) NOT NULL AUTO_INCREMENT,
  hist_ccy_pair varchar(6) NOT NULL,
  hist_date varchar(10) NOT NULL,
  hist_time varchar(5) NOT NULL,
  hist_open decimal(7,5) NOT NULL,
  hist_high decimal(7,5) NOT NULL,
  hist_low decimal(7,5) NOT NULL,
  hist_close decimal(7,5) NOT NULL,
  primary key (id, hist_ccy_pair)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

alter table historical_data add index ccy_pair_date (hist_ccy_pair, hist_date);

ALTER TABLE historical_data
PARTITION BY LIST columns (hist_ccy_pair) (
    PARTITION pAUDCAD VALUES IN('AUDCAD'),
    PARTITION pAUDCHF VALUES IN('AUDCHF'),
    PARTITION pEURCAD VALUES IN('EURCAD'),
    PARTITION pEURGBP VALUES IN('EURGBP'),
	PARTITION pUSDCAD VALUES IN('USDCAD')
);

insert into historical_data (hist_ccy_pair, hist_date, hist_time, hist_open, hist_high, hist_low, hist_close) 
select 'AUDCAD', hist_date, hist_time, hist_open, hist_high, hist_low, hist_close from historical_audcad;

insert into historical_data (hist_ccy_pair, hist_date, hist_time, hist_open, hist_high, hist_low, hist_close) 
select 'AUDCHF', hist_date, hist_time, hist_open, hist_high, hist_low, hist_close from historical_AUDCHF;

insert into historical_data (hist_ccy_pair, hist_date, hist_time, hist_open, hist_high, hist_low, hist_close) 
select 'EURCAD', hist_date, hist_time, hist_open, hist_high, hist_low, hist_close from historical_EURCAD;

insert into historical_data (hist_ccy_pair, hist_date, hist_time, hist_open, hist_high, hist_low, hist_close) 
select 'EURGBP', hist_date, hist_time, hist_open, hist_high, hist_low, hist_close from historical_EURGBP;

insert into historical_data (hist_ccy_pair, hist_date, hist_time, hist_open, hist_high, hist_low, hist_close) 
select 'USDCAD', hist_date, hist_time, hist_open, hist_high, hist_low, hist_close from historical_USDCAD;
