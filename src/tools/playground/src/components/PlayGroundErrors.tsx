import { community } from "wirespec";
import WsError = wirespec.community.flock.wirespec.compiler.lib.WsError;
import { Typography } from "@mui/material";

interface PlayGroundErrorsProps {
    code?: string
    errors?: Array<WsError>
}

export function PlayGroundErrors({errors}: PlayGroundErrorsProps) {
    let errorMessage = 'No errors';
    if (errors != undefined && errors.length > 0) {
      errorMessage = errors.map(error => error.value).join('');
    }
    return (
      <>
        <Typography variant="h5">{errorMessage}</Typography>
      </>
    )
}
