// Wire DTOs stay separate from presentation/domain mapping. OpenAPI is authoritative.
export type PublicUserDto = { id: string; username: string | null; displayName: string | null; avatarUrl: string | null };
export type MatchDto = { user: PublicUserDto; compatibility: number; reasons: { type: string; label: string; count?: number }[] };
export type MatchPageDto = { matches: MatchDto[]; nextCursor: string | null };
export type ApiErrorDto = { code: string; message: string; timestamp: string; path: string };
