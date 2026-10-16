/**
 * Extracts a user-friendly, descriptive error message and status code from an API error response.
 * Compatible with FinFlow GlobalExceptionHandler ErrorResponse format.
 */
export function extractErrorMessage(error) {
  if (!error) {
    return {
      message: 'An unknown error occurred.',
      status: null,
    };
  }

  // Network / Connection Error
  if (error.code === 'ERR_NETWORK' || !error.response) {
    return {
      message: 'Unable to connect to FinFlow server. Please check your network connection or verify that the backend is running.',
      status: 0,
    };
  }

  const { status, data } = error.response;

  // FinFlow GlobalExceptionHandler returns { timestamp, status, error, message, path }
  if (data && typeof data === 'object') {
    if (data.message && typeof data.message === 'string') {
      return {
        message: data.message,
        status: status || data.status,
        errorType: data.error,
      };
    }
    if (data.error && typeof data.error === 'string') {
      return {
        message: data.error,
        status,
      };
    }
  }

  // Fallbacks for standard HTTP status codes
  switch (status) {
    case 400:
      return { message: 'Invalid request data. Please check your inputs.', status };
    case 401:
      return { message: 'Invalid credentials or session expired. Please sign in again.', status };
    case 403:
      return { message: 'Access denied: You are not authorized to perform this action.', status };
    case 404:
      return { message: 'Requested resource was not found.', status };
    case 409:
      return { message: 'A conflict occurred. The resource might already exist.', status };
    case 500:
      return { message: 'Internal server error occurred. Please try again later.', status };
    default:
      return { message: `Request failed with status code ${status}`, status };
  }
}
