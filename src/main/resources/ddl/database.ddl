CREATE TABLE `pares` (
  `id_par` int(11) NOT NULL AUTO_INCREMENT,
  `divisas` varchar(50) NOT NULL,
  `spread` decimal(6,5) NOT NULL,
  `rentabilidad` decimal(3,2) NOT NULL,
  PRIMARY KEY (`id_par`)
) ENGINE=MyISAM AUTO_INCREMENT=79 DEFAULT CHARSET=latin1

CREATE TABLE `historico_audcad` (
  `fecha` varchar(20) NOT NULL,
  `hora` varchar(5) NOT NULL,
  `apertura` decimal(7,5) NOT NULL,
  `alto` decimal(7,5) NOT NULL,
  `bajo` decimal(7,5) NOT NULL,
  `cerrar` decimal(7,5) NOT NULL,
  `volumen` mediumint(9) DEFAULT NULL,
  KEY `fecha_audcad` (`fecha`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1

CREATE INDEX fecha_audcad ON historico_audcad (fecha);
CREATE INDEX fecha_audchf ON historico_audchf (fecha);
CREATE INDEX fecha_eurcad ON historico_eurcad (fecha);
CREATE INDEX fecha_eurgbp ON historico_eurgbp (fecha);
CREATE INDEX fecha_usdcad ON historico_usdcad (fecha);
