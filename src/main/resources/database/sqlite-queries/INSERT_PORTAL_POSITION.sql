INSERT INTO {PortalPosition}
(
   portalName,
   networkName,
   xCoordinate,
   yCoordinate,
   zCoordinate,
   positionType,
   metaData,
   pluginName
)
VALUES
(
   ?,
   ?,
   ?,
   ?,
   ?,
      (
         SELECT
            {PositionType}.id
         FROM
            {PositionType}
         WHERE
            {PositionType}.positionName = ?
      ),
   ?,
   ?
);