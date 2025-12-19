import React, { useState } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import SearchBar from './SearchBar';
import { useSelector } from 'react-redux';
import type { RootState } from '../redux/store';
import UserMenu from './UserMenu';
import AuthLinks from './AuthLinks';
import { Search, Bell } from 'lucide-react';
import { Button } from './ui/button';

const Navbar: React.FC = () => {
  const navigate = useNavigate();
  const [searchLoading, setSearchLoading] = useState(false);
  const [showSearch, setShowSearch] = useState(false);

  const user = useSelector((state: RootState) => state.auth.user);
  const isAuthenticated = !!useSelector((state: RootState) => state.auth.auth?.accessToken);
  const role = useSelector((state: RootState) => state.auth.auth?.role);

  const handleSearch = (query: string, limit: number = 10) => {
    if (!query.trim()) return;

    setSearchLoading(true);
    navigate(`/search?query=${encodeURIComponent(query)}&limit=${limit}`);
    setSearchLoading(false);
    setShowSearch(false);
  };

  return (
    <header className="fixed top-0 left-0 right-0 z-50 bg-gradient-to-b from-black via-black/95 to-transparent">
      <div className="px-4 md:px-12 py-4">
        <div className="flex items-center justify-between gap-8">
          {/* Logo and Nav Links */}
          <div className="flex items-center gap-8">
            <NavLink 
              to="/"
              className="text-red-600 text-2xl font-bold tracking-wider hover:text-red-500 transition-colors"
            >
              BBMOVIE
            </NavLink>
            <nav className="hidden md:flex items-center gap-6">
              <NavLink
                to="/"
                className={({ isActive }) => 
                  `text-sm transition-colors hover:text-gray-300 ${
                    isActive ? 'text-white font-semibold' : 'text-gray-400'
                  }`
                }
              >
                Home
              </NavLink>
              <NavLink
                to="/movies"
                className={({ isActive }) => 
                  `text-sm transition-colors hover:text-gray-300 ${
                    isActive ? 'text-white font-semibold' : 'text-gray-400'
                  }`
                }
              >
                Movies
              </NavLink>
              <NavLink
                to="/categories"
                className={({ isActive }) => 
                  `text-sm transition-colors hover:text-gray-300 ${
                    isActive ? 'text-white font-semibold' : 'text-gray-400'
                  }`
                }
              >
                Categories
              </NavLink>
              {isAuthenticated && (
                <>
                  <NavLink
                    to="/watchlist"
                    className={({ isActive }) => 
                      `text-sm transition-colors hover:text-gray-300 ${
                        isActive ? 'text-white font-semibold' : 'text-gray-400'
                      }`
                    }
                  >
                    My List
                  </NavLink>
                  <NavLink
                    to="/subscriptions"
                    className={({ isActive }) => 
                      `text-sm transition-colors hover:text-gray-300 ${
                        isActive ? 'text-white font-semibold' : 'text-gray-400'
                      }`
                    }
                  >
                    Plans
                  </NavLink>
                </>
              )}
            </nav>
          </div>

          {/* Right Side - Search, Notifications, User */}
          <div className="flex items-center gap-4">
            {showSearch ? (
              <div className="w-64">
                <SearchBar
                  placeholder="Search movies..."
                  onSearch={handleSearch}
                  loading={searchLoading}
                />
              </div>
            ) : (
              <Button 
                variant="ghost" 
                size="icon" 
                className="text-white hover:text-gray-300"
                onClick={() => setShowSearch(true)}
              >
                <Search className="h-5 w-5" />
              </Button>
            )}
            
            {isAuthenticated && (
              <Button variant="ghost" size="icon" className="text-white hover:text-gray-300">
                <Bell className="h-5 w-5" />
              </Button>
            )}
            
            {isAuthenticated ? (
              <UserMenu />
            ) : (
              <AuthLinks />
            )}
          </div>
        </div>
      </div>
    </header>
  );
};

export default Navbar;
