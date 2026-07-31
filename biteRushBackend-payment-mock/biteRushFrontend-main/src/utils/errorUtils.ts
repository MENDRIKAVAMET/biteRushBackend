import { AxiosError } from 'axios';

const getServerErrorText = (status?: number, action: 'login' | 'register' | 'generic' = 'generic') => {
  switch (status) {
    case 400:
      return action === 'login'
        ? 'Vérifiez votre email et votre mot de passe, puis réessayez.'
        : 'Vérifiez que tous les champs sont correctement remplis et réessayez.';
    case 401:
      return 'Email ou mot de passe incorrect. Réessayez.';
    case 403:
      return 'Accès refusé. Vérifiez vos identifiants ou contactez le support.';
    case 409:
      return 'Ce compte existe déjà. Essayez de vous connecter ou utilisez un autre email.';
    case 422:
      return action === 'register'
        ? 'Les informations d’inscription ne sont pas valides. Vérifiez le formulaire.'
        : 'Les informations saisies sont invalides. Vérifiez et réessayez.';
    case 500:
    case 502:
    case 503:
    case 504:
      return 'Le service est temporairement indisponible. Réessayez plus tard.';
    default:
      return 'Une erreur est survenue. Réessayez ultérieurement.';
  }
};

export const getFriendlyErrorMessage = (
  error: unknown,
  action: 'login' | 'register' | 'generic' = 'generic'
): string => {
  if (!error) {
    return getServerErrorText(undefined, action);
  }

  if (error instanceof Error) {
    const axiosError = error as AxiosError;
    const response = axiosError.response;
    let serverMessage: string | undefined;

    if (response?.data && typeof response.data === 'object' && 'message' in response.data) {
      serverMessage = (response.data as any).message;
    } else if (response?.data && typeof response.data === 'string') {
      serverMessage = response.data;
    }

    if (serverMessage) {
      return String(serverMessage);
    }

    return getServerErrorText(response?.status, action);
  }

  if (typeof error === 'string') {
    return error;
  }

  return getServerErrorText(undefined, action);
};
