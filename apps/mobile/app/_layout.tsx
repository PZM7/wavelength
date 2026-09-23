import React from 'react';
import { Stack } from 'expo-router';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { StatusBar } from 'expo-status-bar';
import { SessionProvider } from '../src/features/auth/SessionProvider';
import { colors } from '../src/components/ui';
import { ApiError } from '../src/api/http';

const queries = new QueryClient({ defaultOptions: { queries: { staleTime: 30_000, retry: (count, error) => count < 1 && !(error instanceof ApiError && error.status >= 400 && error.status < 500) } } });
export default function RootLayout() {
  return <SafeAreaProvider><QueryClientProvider client={queries}><SessionProvider>
    <StatusBar style="light" />
    <Stack screenOptions={{ headerShown: false, contentStyle: { backgroundColor: colors.bg } }} />
  </SessionProvider></QueryClientProvider></SafeAreaProvider>;
}
